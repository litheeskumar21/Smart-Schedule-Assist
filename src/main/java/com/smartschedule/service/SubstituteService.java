package com.smartschedule.service;

import com.smartschedule.exception.ResourceNotFoundException;
import com.smartschedule.model.*;
import com.smartschedule.model.enums.ClassResolution;
import com.smartschedule.model.enums.EntryStatus;
import com.smartschedule.model.enums.NotificationType;
import com.smartschedule.repository.AbsenceAffectedClassRepository;
import com.smartschedule.repository.SubstituteAssignmentRepository;
import com.smartschedule.repository.TeacherRepository;
import com.smartschedule.repository.TimetableEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class SubstituteService {
    private final AbsenceAffectedClassRepository absenceAffectedClassRepository;
    private final SubstituteAssignmentRepository substituteAssignmentRepository;
    private final TeacherRepository teacherRepository;
    private final TimetableEntryRepository timetableEntryRepository;
    private final TeacherService teacherService;
    private final NotificationService notificationService;

    public SubstituteService(AbsenceAffectedClassRepository absenceAffectedClassRepository,
                             SubstituteAssignmentRepository substituteAssignmentRepository,
                             TeacherRepository teacherRepository,
                             TimetableEntryRepository timetableEntryRepository,
                             TeacherService teacherService,
                             NotificationService notificationService) {
        this.absenceAffectedClassRepository = absenceAffectedClassRepository;
        this.substituteAssignmentRepository = substituteAssignmentRepository;
        this.teacherRepository = teacherRepository;
        this.timetableEntryRepository = timetableEntryRepository;
        this.teacherService = teacherService;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<Teacher> getAvailableSubstitutes(DayOfWeek day, Period period, Long excludeTeacherId) {
        List<Teacher> available = teacherService.getAvailableTeachers(day, period);
        if (excludeTeacherId != null) {
            return available.stream()
                    .filter(t -> !t.getId().equals(excludeTeacherId))
                    .collect(Collectors.toList());
        }
        return available;
    }

    public SubstituteAssignment assignSubstitute(Long affectedClassId, Long substituteTeacherId, User admin) {
        AbsenceAffectedClass affectedClass = absenceAffectedClassRepository.findById(affectedClassId)
                .orElseThrow(() -> new ResourceNotFoundException("Affected class not found with ID: " + affectedClassId));

        Teacher substitute = teacherRepository.findById(substituteTeacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Substitute teacher not found with ID: " + substituteTeacherId));

        // Create or update substitute assignment
        SubstituteAssignment assignment = substituteAssignmentRepository.findByAffectedClass(affectedClass)
                .orElse(new SubstituteAssignment());
        assignment.setAffectedClass(affectedClass);
        assignment.setSubstituteTeacher(substitute);
        assignment.setAssignedBy(admin);
        SubstituteAssignment saved = substituteAssignmentRepository.save(assignment);

        // Update affected class resolution
        affectedClass.setResolution(ClassResolution.SUBSTITUTE_ASSIGNED);
        absenceAffectedClassRepository.save(affectedClass);

        // Update timetable entry
        TimetableEntry entry = affectedClass.getTimetableEntry();
        if (entry != null) {
            entry.setStatus(EntryStatus.SUBSTITUTE);
            entry.setSubstituteTeacher(substitute);
            timetableEntryRepository.save(entry);

            // Notify substitute teacher
            String subMsg = "You have been assigned as a substitute teacher for " +
                    (entry.getSubject() != null ? entry.getSubject().getName() : "a class") +
                    " (" + entry.getSection().getDisplayName() + ") on " +
                    affectedClass.getAbsenceRequest().getAbsenceDate() + " - Period " +
                    entry.getPeriod().getPeriodNumber();
            notificationService.notifyTeacher(substitute, "Substitute Assignment", subMsg, NotificationType.SUBSTITUTE_ASSIGNED);

            // Notify students in the affected section
            String studentMsg = (entry.getSubject() != null ? entry.getSubject().getName() : "Class") +
                    " on " + affectedClass.getAbsenceRequest().getAbsenceDate() +
                    " (Period " + entry.getPeriod().getPeriodNumber() + ") will be taken by " +
                    substitute.getUser().getFullName() + " (Substitute).";
            notificationService.notifyStudentsInSection(entry.getSection(), "Substitute Teacher Assigned", studentMsg, NotificationType.SUBSTITUTE_ASSIGNED);
        }

        return saved;
    }

    public void cancelClass(Long affectedClassId, User admin) {
        AbsenceAffectedClass affectedClass = absenceAffectedClassRepository.findById(affectedClassId)
                .orElseThrow(() -> new ResourceNotFoundException("Affected class not found with ID: " + affectedClassId));

        affectedClass.setResolution(ClassResolution.CANCELLED);
        absenceAffectedClassRepository.save(affectedClass);

        TimetableEntry entry = affectedClass.getTimetableEntry();
        if (entry != null) {
            entry.setStatus(EntryStatus.CANCELLED);
            timetableEntryRepository.save(entry);

            // Notify students in section
            String studentMsg = (entry.getSubject() != null ? entry.getSubject().getName() : "Class") +
                    " on " + affectedClass.getAbsenceRequest().getAbsenceDate() +
                    " (Period " + entry.getPeriod().getPeriodNumber() + ") has been CANCELLED.";
            notificationService.notifyStudentsInSection(entry.getSection(), "Class Cancelled", studentMsg, NotificationType.CLASS_CANCELLATION);
        }
    }
}
