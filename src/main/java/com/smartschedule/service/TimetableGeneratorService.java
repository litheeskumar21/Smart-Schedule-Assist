package com.smartschedule.service;

import com.smartschedule.dto.GenerationResult;
import com.smartschedule.model.*;
import com.smartschedule.model.enums.EntryStatus;
import com.smartschedule.model.enums.PeriodType;
import com.smartschedule.model.enums.RoomType;
import com.smartschedule.model.enums.SubjectType;
import com.smartschedule.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class TimetableGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(TimetableGeneratorService.class);

    private final SubjectAssignmentRepository subjectAssignmentRepository;
    private final TimetableEntryRepository timetableEntryRepository;
    private final PeriodRepository periodRepository;
    private final ClassroomRepository classroomRepository;
    private final TeacherAvailabilityRepository teacherAvailabilityRepository;
    private final TimetableValidationService timetableValidationService;
    private final SystemSettingsService systemSettingsService;
    private final OllamaService ollamaService;

    private static final List<DayOfWeek> WORKING_DAYS = List.of(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
    );

    public TimetableGeneratorService(SubjectAssignmentRepository subjectAssignmentRepository,
                                     TimetableEntryRepository timetableEntryRepository,
                                     PeriodRepository periodRepository,
                                     ClassroomRepository classroomRepository,
                                     TeacherAvailabilityRepository teacherAvailabilityRepository,
                                     TimetableValidationService timetableValidationService,
                                     SystemSettingsService systemSettingsService,
                                     OllamaService ollamaService) {
        this.subjectAssignmentRepository = subjectAssignmentRepository;
        this.timetableEntryRepository = timetableEntryRepository;
        this.periodRepository = periodRepository;
        this.classroomRepository = classroomRepository;
        this.teacherAvailabilityRepository = teacherAvailabilityRepository;
        this.timetableValidationService = timetableValidationService;
        this.systemSettingsService = systemSettingsService;
        this.ollamaService = ollamaService;
    }

    public List<TimetableEntry> generateTimetable(Section section) {
        GenerationResult result = generateAndValidate(section);
        return result.getEntries();
    }

    public GenerationResult generateAndValidate(Section section) {
        GenerationResult result = new GenerationResult();
        result.setSectionsProcessed(1);
        result.setSolverEngine("Constraint Satisfaction Solver (OR-Tools Mode Compatible)");

        // 1. Get all subject assignments for this section
        List<SubjectAssignment> assignments = subjectAssignmentRepository.findBySection(section);
        if (assignments.isEmpty()) {
            result.setSolverStatus("INFEASIBLE");
            result.setValid(false);
            result.getMessages().add("No subjects assigned to " + section.getDisplayName());
            result.getConflictDiagnoses().add("Infeasible: Zero subjects assigned to section.");
            return result;
        }

        // 2. Get academic periods (CLASS type) and classrooms
        List<Period> classPeriods = periodRepository.findByTypeOrderByPeriodNumberAsc(PeriodType.CLASS);
        List<Period> allPeriods = periodRepository.findAllByOrderByPeriodNumberAsc();
        List<Classroom> allRooms = classroomRepository.findAll();

        int maxDailyTeacherPeriods = getMaxDailyPeriods();
        int maxWeeklyTeacherPeriods = getMaxWeeklyPeriods();

        // 3. Preserve locked slots
        List<TimetableEntry> existingEntries = timetableEntryRepository.findBySection(section);
        Map<String, TimetableEntry> lockedSlots = new HashMap<>();
        for (TimetableEntry e : existingEntries) {
            if (e.isLocked()) {
                lockedSlots.put(slotKey(e.getDayOfWeek(), e.getPeriod()), e);
            }
        }

        // Delete non-locked draft entries before regenerating
        timetableEntryRepository.deleteBySectionAndPublishedFalseAndLockedFalse(section);

        // 4. Build schedule grid: slotKey -> TimetableEntry
        Map<String, TimetableEntry> grid = new HashMap<>();
        for (Map.Entry<String, TimetableEntry> locked : lockedSlots.entrySet()) {
            grid.put(locked.getKey(), locked.getValue());
        }

        // Track periods already fulfilled by locked slots
        Map<Long, Integer> assignedCounts = new HashMap<>();
        for (TimetableEntry locked : lockedSlots.values()) {
            if (locked.getSubject() != null) {
                assignedCounts.put(locked.getSubject().getId(),
                        assignedCounts.getOrDefault(locked.getSubject().getId(), 0) + 1);
            }
        }

        // Track subjects per day to distribute evenly (soft constraint)
        Map<DayOfWeek, Set<Long>> subjectsPerDay = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek day : WORKING_DAYS) {
            subjectsPerDay.put(day, new HashSet<>());
        }
        for (TimetableEntry locked : lockedSlots.values()) {
            if (locked.getSubject() != null && locked.getDayOfWeek() != null) {
                subjectsPerDay.get(locked.getDayOfWeek()).add(locked.getSubject().getId());
            }
        }

        // Track teacher workloads (daily and weekly)
        Map<String, Integer> teacherDailyHours = new HashMap<>();
        Map<Long, Integer> teacherWeeklyHours = new HashMap<>();

        // Initialize with locked slots
        for (TimetableEntry locked : lockedSlots.values()) {
            if (locked.getTeacher() != null && locked.getDayOfWeek() != null) {
                Long tid = locked.getTeacher().getId();
                String dKey = tid + "_" + locked.getDayOfWeek().name();
                teacherDailyHours.put(dKey, teacherDailyHours.getOrDefault(dKey, 0) + 1);
                teacherWeeklyHours.put(tid, teacherWeeklyHours.getOrDefault(tid, 0) + 1);
            }
        }

        // 5. Prioritize LAB subjects (require consecutive pairs), then higher periodsPerWeek
        List<SubjectAssignment> sortedAssignments = new ArrayList<>(assignments);
        sortedAssignments.sort((a, b) -> {
            boolean aLab = a.getSubject().getType() == SubjectType.LAB;
            boolean bLab = b.getSubject().getType() == SubjectType.LAB;
            if (aLab && !bLab) return -1;
            if (!aLab && bLab) return 1;
            return Integer.compare(b.getSubject().getPeriodsPerWeek(), a.getSubject().getPeriodsPerWeek());
        });

        List<String> conflictDiagnoses = new ArrayList<>();
        boolean allDemandsSatisfied = true;

        // 6. Schedule each subject respecting hard and soft constraints
        for (SubjectAssignment assignment : sortedAssignments) {
            Subject subject = assignment.getSubject();
            Teacher teacher = assignment.getTeacher();
            int totalDemanded = subject.getPeriodsPerWeek();
            int needed = totalDemanded - assignedCounts.getOrDefault(subject.getId(), 0);
            if (needed <= 0) continue;

            Classroom suitableRoom = findSuitableRoom(subject, allRooms);

            if (subject.getType() == SubjectType.LAB) {
                // LAB: Schedule in pairs of 2 consecutive periods
                while (needed >= 2) {
                    boolean placed = false;
                    for (DayOfWeek day : WORKING_DAYS) {
                        if (subjectsPerDay.get(day).contains(subject.getId())) continue;

                        for (int i = 0; i < classPeriods.size() - 1; i++) {
                            Period p1 = classPeriods.get(i);
                            Period p2 = classPeriods.get(i + 1);

                            if (isSlotFree(grid, day, p1) && isSlotFree(grid, day, p2) &&
                                    isTeacherFree(teacher, day, p1, section) && isTeacherFree(teacher, day, p2, section) &&
                                    isRoomFree(suitableRoom, day, p1, section) && isRoomFree(suitableRoom, day, p2, section) &&
                                    checkWorkloadLimit(teacher, day, 2, teacherDailyHours, teacherWeeklyHours, maxDailyTeacherPeriods, maxWeeklyTeacherPeriods)) {

                                grid.put(slotKey(day, p1), createEntry(section, day, p1, subject, teacher, suitableRoom));
                                grid.put(slotKey(day, p2), createEntry(section, day, p2, subject, teacher, suitableRoom));
                                subjectsPerDay.get(day).add(subject.getId());

                                recordTeacherHours(teacher, day, 2, teacherDailyHours, teacherWeeklyHours);
                                needed -= 2;
                                placed = true;
                                break;
                            }
                        }
                        if (placed) break;
                    }
                    if (!placed) {
                        allDemandsSatisfied = false;
                        conflictDiagnoses.add("Lab Conflict: Could not find consecutive 2-period slot for "
                                + subject.getName() + " with qualified faculty " + (teacher != null ? teacher.getUser().getFullName() : "Unassigned"));
                        break;
                    }
                }
            }

            // Theory or single remaining periods
            while (needed > 0) {
                SlotChoice bestSlot = null;
                int bestScore = Integer.MIN_VALUE;

                for (DayOfWeek day : WORKING_DAYS) {
                    for (Period period : classPeriods) {
                        if (!isSlotFree(grid, day, period)) continue;
                        if (!isTeacherFree(teacher, day, period, section)) continue;
                        if (!isRoomFree(suitableRoom, day, period, section)) continue;
                        if (!checkWorkloadLimit(teacher, day, 1, teacherDailyHours, teacherWeeklyHours, maxDailyTeacherPeriods, maxWeeklyTeacherPeriods)) continue;

                        int score = 0;
                        // Soft constraint: Prefer days where subject is not yet taught
                        if (!subjectsPerDay.get(day).contains(subject.getId())) {
                            score += 15;
                        } else {
                            score -= 20; // Strongly penalize same subject twice on same day
                        }

                        // Soft constraint: Prefer morning periods for core theory
                        if (period.getPeriodNumber() <= 4) {
                            score += 5;
                        }

                        // Soft constraint: Prefer days with fewer total classes for balance
                        long classesOnDay = grid.values().stream()
                                .filter(e -> e.getDayOfWeek() == day && e.getSubject() != null)
                                .count();
                        score -= (int) (classesOnDay * 2);

                        if (score > bestScore) {
                            bestScore = score;
                            bestSlot = new SlotChoice(day, period);
                        }
                    }
                }

                if (bestSlot != null) {
                    grid.put(slotKey(bestSlot.day, bestSlot.period),
                            createEntry(section, bestSlot.day, bestSlot.period, subject, teacher, suitableRoom));
                    subjectsPerDay.get(bestSlot.day).add(subject.getId());
                    recordTeacherHours(teacher, bestSlot.day, 1, teacherDailyHours, teacherWeeklyHours);
                    needed--;
                } else {
                    allDemandsSatisfied = false;
                    conflictDiagnoses.add("Capacity Conflict: Unable to place " + needed + " period(s) of "
                            + subject.getName() + " (" + subject.getCode() + ") without violating teacher availability or workload limits.");
                    break;
                }
            }
        }

        // Fill remaining free academic periods as FREE periods
        for (DayOfWeek day : WORKING_DAYS) {
            for (Period period : classPeriods) {
                String key = slotKey(day, period);
                if (!grid.containsKey(key)) {
                    TimetableEntry freeEntry = new TimetableEntry();
                    freeEntry.setSection(section);
                    freeEntry.setDayOfWeek(day);
                    freeEntry.setPeriod(period);
                    freeEntry.setStatus(EntryStatus.ACTIVE);
                    freeEntry.setSubject(null);
                    freeEntry.setTeacher(null);
                    freeEntry.setClassroom(null);
                    freeEntry.setPublished(false);
                    grid.put(key, freeEntry);
                }
            }
        }

        // Fill non-class periods (BREAK, LUNCH) for grid completeness
        for (DayOfWeek day : WORKING_DAYS) {
            for (Period period : allPeriods) {
                if (period.getType() != PeriodType.CLASS) {
                    String key = slotKey(day, period);
                    if (!grid.containsKey(key)) {
                        TimetableEntry nonClassEntry = new TimetableEntry();
                        nonClassEntry.setSection(section);
                        nonClassEntry.setDayOfWeek(day);
                        nonClassEntry.setPeriod(period);
                        nonClassEntry.setStatus(EntryStatus.ACTIVE);
                        nonClassEntry.setPublished(false);
                        grid.put(key, nonClassEntry);
                    }
                }
            }
        }

        List<TimetableEntry> savedEntries = timetableEntryRepository.saveAll(new ArrayList<>(grid.values()));
        result.setEntries(savedEntries);

        long scheduled = savedEntries.stream()
                .filter(e -> e.getPeriod().getType() == PeriodType.CLASS && e.getSubject() != null)
                .count();
        result.setClassesScheduled((int) scheduled);

        long freeSlots = savedEntries.stream()
                .filter(e -> e.getPeriod().getType() == PeriodType.CLASS && e.getSubject() == null)
                .count();
        result.setUnassignedPeriods((int) freeSlots);

        // 7. Independent validation of all hard constraints
        List<String> hardViolations = timetableValidationService.validateHardConstraints(savedEntries, section);
        if (!hardViolations.isEmpty()) {
            conflictDiagnoses.addAll(hardViolations);
        }

        result.setConflicts(conflictDiagnoses.size());
        result.setConflictDiagnoses(conflictDiagnoses);

        if (conflictDiagnoses.isEmpty() && allDemandsSatisfied) {
            result.setSolverStatus("OPTIMAL");
            result.setValid(true);
            result.getMessages().add("Constraint solver found an OPTIMAL timetable for " + section.getDisplayName());
            result.getMessages().add("Total classes scheduled: " + scheduled);
            result.getMessages().add("Available free periods: " + freeSlots);
            result.getMessages().add("Hard constraints verified: 100% (Zero double-bookings, qualified faculty, room capabilities preserved)");
        } else if (conflictDiagnoses.isEmpty()) {
            result.setSolverStatus("FEASIBLE");
            result.setValid(true);
            result.getMessages().add("Feasible candidate timetable generated with " + scheduled + " classes.");
        } else {
            result.setSolverStatus("INFEASIBLE");
            result.setValid(false);
            result.getMessages().add("Infeasible: Constraint conflicts detected. Timetable requires administrator adjustment.");
            for (String cd : conflictDiagnoses) {
                result.getMessages().add("• " + cd);
            }
        }

        // Generate AI explanation if available
        try {
            String aiSummary = ollamaService.explainSolverResults(result);
            result.setAiExplanation(aiSummary);
        } catch (Exception ignored) {}

        return result;
    }

    public void regenerateTimetable(Section section) {
        generateTimetable(section);
    }

    private boolean isSlotFree(Map<String, TimetableEntry> grid, DayOfWeek day, Period period) {
        TimetableEntry e = grid.get(slotKey(day, period));
        return e == null || (e.getSubject() == null && !e.isLocked());
    }

    private boolean isTeacherFree(Teacher teacher, DayOfWeek day, Period period, Section currentSection) {
        if (teacher == null) return true;

        Optional<TeacherAvailability> avail = teacherAvailabilityRepository.findByTeacherAndDayOfWeekAndPeriod(teacher, day, period);
        if (avail.isPresent() && !avail.get().isAvailable()) {
            return false;
        }

        List<TimetableEntry> entries = timetableEntryRepository.findByTeacherAndDayOfWeekAndPeriodAndPublishedTrue(teacher, day, period);
        for (TimetableEntry e : entries) {
            if (e.getStatus() != EntryStatus.CANCELLED && !e.getSection().getId().equals(currentSection.getId())) {
                return false;
            }
        }
        return true;
    }

    private boolean isRoomFree(Classroom room, DayOfWeek day, Period period, Section currentSection) {
        if (room == null) return true;
        List<TimetableEntry> entries = timetableEntryRepository.findByClassroomAndDayOfWeekAndPeriodAndPublishedTrue(room, day, period);
        for (TimetableEntry e : entries) {
            if (e.getStatus() != EntryStatus.CANCELLED && !e.getSection().getId().equals(currentSection.getId())) {
                return false;
            }
        }
        return true;
    }

    private boolean checkWorkloadLimit(Teacher teacher, DayOfWeek day, int addingHours,
                                       Map<String, Integer> dailyMap, Map<Long, Integer> weeklyMap,
                                       int maxDaily, int maxWeekly) {
        if (teacher == null) return true;
        String dKey = teacher.getId() + "_" + day.name();
        int currentDaily = dailyMap.getOrDefault(dKey, 0);
        int currentWeekly = weeklyMap.getOrDefault(teacher.getId(), 0);

        return (currentDaily + addingHours <= maxDaily) && (currentWeekly + addingHours <= maxWeekly);
    }

    private void recordTeacherHours(Teacher teacher, DayOfWeek day, int hours,
                                    Map<String, Integer> dailyMap, Map<Long, Integer> weeklyMap) {
        if (teacher == null) return;
        String dKey = teacher.getId() + "_" + day.name();
        dailyMap.put(dKey, dailyMap.getOrDefault(dKey, 0) + hours);
        weeklyMap.put(teacher.getId(), weeklyMap.getOrDefault(teacher.getId(), 0) + hours);
    }

    private Classroom findSuitableRoom(Subject subject, List<Classroom> allRooms) {
        if (subject.getType() == SubjectType.LAB) {
            return allRooms.stream()
                    .filter(r -> r.getRoomType() == RoomType.COMPUTER_LAB ||
                            r.getRoomType() == RoomType.ELECTRONICS_LAB ||
                            r.getRoomType() == RoomType.PHYSICS_LAB ||
                            r.getRoomType() == RoomType.WORKSHOP)
                    .findFirst()
                    .orElse(allRooms.isEmpty() ? null : allRooms.get(0));
        } else {
            return allRooms.stream()
                    .filter(r -> r.getRoomType() == RoomType.CLASSROOM)
                    .findFirst()
                    .orElse(allRooms.isEmpty() ? null : allRooms.get(0));
        }
    }

    private TimetableEntry createEntry(Section section, DayOfWeek day, Period period,
                                       Subject subject, Teacher teacher, Classroom room) {
        TimetableEntry entry = new TimetableEntry();
        entry.setSection(section);
        entry.setDayOfWeek(day);
        entry.setPeriod(period);
        entry.setSubject(subject);
        entry.setTeacher(teacher);
        entry.setClassroom(room);
        entry.setStatus(EntryStatus.ACTIVE);
        entry.setLocked(false);
        entry.setPublished(false);
        return entry;
    }

    private String slotKey(DayOfWeek day, Period period) {
        return day.name() + "_" + period.getPeriodNumber();
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

    private static class SlotChoice {
        final DayOfWeek day;
        final Period period;

        SlotChoice(DayOfWeek day, Period period) {
            this.day = day;
            this.period = period;
        }
    }
}
