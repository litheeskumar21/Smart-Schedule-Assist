package com.smartschedule.service;

import com.smartschedule.model.Section;
import com.smartschedule.model.SubjectAssignment;
import com.smartschedule.model.TimetableEntry;
import com.smartschedule.repository.SubjectAssignmentRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TimetableValidationService {
    private final SubjectAssignmentRepository subjectAssignmentRepository;

    public TimetableValidationService(SubjectAssignmentRepository subjectAssignmentRepository) {
        this.subjectAssignmentRepository = subjectAssignmentRepository;
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

    public List<String> validatePostGeneration(List<TimetableEntry> entries) {
        List<String> warnings = new ArrayList<>();
        if (entries.isEmpty()) {
            warnings.add("No timetable entries were generated.");
        }
        return warnings;
    }
}
