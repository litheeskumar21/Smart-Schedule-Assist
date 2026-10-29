package com.smartschedule.service;

import com.smartschedule.model.*;
import com.smartschedule.model.enums.EntryStatus;
import com.smartschedule.model.enums.PeriodType;
import com.smartschedule.model.enums.RoomType;
import com.smartschedule.model.enums.SubjectType;
import com.smartschedule.repository.SubjectAssignmentRepository;
import com.smartschedule.repository.TeacherAvailabilityRepository;
import com.smartschedule.repository.TimetableEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class TimetableValidationService {

    private final SubjectAssignmentRepository subjectAssignmentRepository;
    private final TimetableEntryRepository timetableEntryRepository;
    private final TeacherAvailabilityRepository teacherAvailabilityRepository;
    private final SystemSettingsService systemSettingsService;

    public TimetableValidationService(SubjectAssignmentRepository subjectAssignmentRepository,
                                      TimetableEntryRepository timetableEntryRepository,
                                      TeacherAvailabilityRepository teacherAvailabilityRepository,
                                      SystemSettingsService systemSettingsService) {
        this.subjectAssignmentRepository = subjectAssignmentRepository;
        this.timetableEntryRepository = timetableEntryRepository;
        this.teacherAvailabilityRepository = teacherAvailabilityRepository;
        this.systemSettingsService = systemSettingsService;
    }

    public List<String> validatePreGeneration(Section section) {
        List<String> errors = new ArrayList<>();
        List<SubjectAssignment> assignments = subjectAssignmentRepository.findBySection(section);

        if (assignments.isEmpty()) {
            errors.add("No subjects are assigned to section " + section.getDisplayName() + ". Please assign subjects before generating a timetable.");
            return errors;
        }

        int totalPeriods = 0;
        for (SubjectAssignment assignment : assignments) {
            if (assignment.getTeacher() == null) {
                errors.add("Subject " + assignment.getSubject().getName() + " (" + assignment.getSubject().getCode() + ") has no teacher assigned for section " + section.getDisplayName());
            }
            if (assignment.getSubject().getPeriodsPerWeek() <= 0) {
                errors.add("Subject " + assignment.getSubject().getName() + " requires at least 1 period per week.");
            }
            totalPeriods += assignment.getSubject().getPeriodsPerWeek();
        }

        // Max academic periods per week = 6 periods/day * 5 days = 30
        if (totalPeriods > 30) {
            errors.add("Total required periods (" + totalPeriods + ") exceeds available weekly academic periods (30).");
        }

        return errors;
    }

    /**
     * Complete independent validation of all hard constraints for candidate timetable entries.
     * Returns list of hard constraint violations (if empty, timetable is 100% valid).
     */
    public List<String> validateHardConstraints(List<TimetableEntry> entries, Section currentSection) {
        List<String> violations = new ArrayList<>();
        if (entries == null || entries.isEmpty()) {
            violations.add("Candidate timetable is empty.");
            return violations;
        }

        Map<String, TimetableEntry> teacherSlots = new HashMap<>();
        Map<String, TimetableEntry> roomSlots = new HashMap<>();
        Map<String, TimetableEntry> sectionSlots = new HashMap<>();

        Map<Long, Integer> teacherWeeklyLoad = new HashMap<>();
        Map<String, Integer> teacherDailyLoad = new HashMap<>();

        int maxDaily = getMaxDailyPeriods();
        int maxWeekly = getMaxWeeklyPeriods();

        for (TimetableEntry entry : entries) {
            if (entry.getPeriod() == null || entry.getPeriod().getType() != PeriodType.CLASS || entry.getSubject() == null) {
                continue;
            }

            DayOfWeek day = entry.getDayOfWeek();
            Period period = entry.getPeriod();
            Teacher teacher = entry.getTeacher();
            Classroom room = entry.getClassroom();
            Subject subject = entry.getSubject();

            String timeSlotKey = day.name() + "_P" + period.getPeriodNumber();

            // 1. Class / Section double-booking
            String secKey = (entry.getSection() != null ? entry.getSection().getId() : "null") + "_" + timeSlotKey;
            if (sectionSlots.containsKey(secKey)) {
                violations.add("Conflict: Section " + (entry.getSection() != null ? entry.getSection().getDisplayName() : "")
                        + " is scheduled for two classes simultaneously at " + day + " Period " + period.getPeriodNumber());
            } else {
                sectionSlots.put(secKey, entry);
            }

            // 2. Teacher double-booking (within this section schedule)
            if (teacher != null) {
                String teacherKey = teacher.getId() + "_" + timeSlotKey;
                if (teacherSlots.containsKey(teacherKey)) {
                    violations.add("Conflict: Teacher " + teacher.getUser().getFullName()
                            + " is double-booked at " + day + " Period " + period.getPeriodNumber());
                } else {
                    teacherSlots.put(teacherKey, entry);
                }

                // Check external teacher double-booking (other published sections)
                List<TimetableEntry> externalClasses = timetableEntryRepository
                        .findByTeacherAndDayOfWeekAndPeriodAndPublishedTrue(teacher, day, period);
                for (TimetableEntry ext : externalClasses) {
                    if (ext.getStatus() != EntryStatus.CANCELLED &&
                            (currentSection == null || ext.getSection() == null || !ext.getSection().getId().equals(currentSection.getId()))) {
                        violations.add("Conflict: Teacher " + teacher.getUser().getFullName()
                                + " is already assigned to " + ext.getSection().getDisplayName()
                                + " at " + day + " Period " + period.getPeriodNumber());
                    }
                }

                // Teacher availability matrix check
                Optional<TeacherAvailability> avail = teacherAvailabilityRepository
                        .findByTeacherAndDayOfWeekAndPeriod(teacher, day, period);
                if (avail.isPresent() && !avail.get().isAvailable()) {
                    violations.add("Availability Violation: Teacher " + teacher.getUser().getFullName()
                            + " has declared unavailability at " + day + " Period " + period.getPeriodNumber());
                }

                // Track workloads
                teacherWeeklyLoad.put(teacher.getId(), teacherWeeklyLoad.getOrDefault(teacher.getId(), 0) + 1);
                String dailyKey = teacher.getId() + "_" + day.name();
                teacherDailyLoad.put(dailyKey, teacherDailyLoad.getOrDefault(dailyKey, 0) + 1);

                if (teacherDailyLoad.get(dailyKey) > maxDaily) {
                    violations.add("Workload Violation: Teacher " + teacher.getUser().getFullName()
                            + " exceeds maximum daily limit of " + maxDaily + " classes on " + day);
                }
            }

            // 3. Room double-booking
            if (room != null) {
                String roomKey = room.getId() + "_" + timeSlotKey;
                if (roomSlots.containsKey(roomKey)) {
                    violations.add("Conflict: Room " + room.getName()
                            + " is double-booked at " + day + " Period " + period.getPeriodNumber());
                } else {
                    roomSlots.put(roomKey, entry);
                }

                // Check room capability
                if (subject.getType() == SubjectType.LAB) {
                    if (room.getRoomType() == RoomType.CLASSROOM) {
                        violations.add("Capability Mismatch: Lab course '" + subject.getName()
                                + "' cannot be scheduled in standard lecture room '" + room.getName() + "'");
                    }
                }
            }
        }

        // Check weekly workload limit
        for (Map.Entry<Long, Integer> wl : teacherWeeklyLoad.entrySet()) {
            if (wl.getValue() > maxWeekly) {
                violations.add("Workload Violation: Teacher ID " + wl.getKey()
                        + " exceeds maximum weekly teaching limit (" + wl.getValue() + "/" + maxWeekly + " periods)");
            }
        }

        return violations;
    }

    public List<String> validatePostGeneration(List<TimetableEntry> entries) {
        return validateHardConstraints(entries, null);
    }

    private int getMaxDailyPeriods() {
        try {
            return Integer.parseInt(systemSettingsService.getSetting("max_daily_teacher_periods", "4"));
        } catch (Exception e) {
            return 4;
        }
    }

    private int getMaxWeeklyPeriods() {
        try {
            return Integer.parseInt(systemSettingsService.getSetting("max_weekly_teacher_periods", "18"));
        } catch (Exception e) {
            return 18;
        }
    }
}
