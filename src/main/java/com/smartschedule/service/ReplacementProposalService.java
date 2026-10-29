package com.smartschedule.service;

import com.smartschedule.dto.ReplacementPlanDTO;
import com.smartschedule.model.*;
import com.smartschedule.model.enums.EntryStatus;
import com.smartschedule.model.enums.RequestStatus;
import com.smartschedule.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class ReplacementProposalService {

    private static final Logger log = LoggerFactory.getLogger(ReplacementProposalService.class);

    private final AbsenceAffectedClassRepository absenceAffectedClassRepository;
    private final TeacherRepository teacherRepository;
    private final TimetableEntryRepository timetableEntryRepository;
    private final TeacherAvailabilityRepository teacherAvailabilityRepository;
    private final AbsenceRequestRepository absenceRequestRepository;
    private final SubjectAssignmentRepository subjectAssignmentRepository;
    private final SystemSettingsService systemSettingsService;
    private final OllamaService ollamaService;

    public ReplacementProposalService(AbsenceAffectedClassRepository absenceAffectedClassRepository,
                                      TeacherRepository teacherRepository,
                                      TimetableEntryRepository timetableEntryRepository,
                                      TeacherAvailabilityRepository teacherAvailabilityRepository,
                                      AbsenceRequestRepository absenceRequestRepository,
                                      SubjectAssignmentRepository subjectAssignmentRepository,
                                      SystemSettingsService systemSettingsService,
                                      OllamaService ollamaService) {
        this.absenceAffectedClassRepository = absenceAffectedClassRepository;
        this.teacherRepository = teacherRepository;
        this.timetableEntryRepository = timetableEntryRepository;
        this.teacherAvailabilityRepository = teacherAvailabilityRepository;
        this.absenceRequestRepository = absenceRequestRepository;
        this.subjectAssignmentRepository = subjectAssignmentRepository;
        this.systemSettingsService = systemSettingsService;
        this.ollamaService = ollamaService;
    }

    public int getMaxDailyPeriods() {
        try {
            return Integer.parseInt(systemSettingsService.getSetting("max_daily_teacher_periods", "4"));
        } catch (Exception e) {
            return 4;
        }
    }

    public int getMaxWeeklyPeriods() {
        try {
            return Integer.parseInt(systemSettingsService.getSetting("max_weekly_teacher_periods", "18"));
        } catch (Exception e) {
            return 18;
        }
    }

    /**
     * Calculates an optimal, constraint-verified replacement plan for all affected classes
     * of an absence request.
     */
    public ReplacementPlanDTO calculateReplacementPlan(AbsenceRequest request) {
        ReplacementPlanDTO plan = new ReplacementPlanDTO();
        plan.setAbsenceRequestId(request.getId());
        plan.setTeacherName(request.getTeacher().getUser() != null ? request.getTeacher().getUser().getFullName() : "Faculty");

        List<AbsenceAffectedClass> affectedClasses = absenceAffectedClassRepository.findByAbsenceRequest(request);
        plan.setTotalAffectedClasses(affectedClasses.size());

        if (affectedClasses.isEmpty()) {
            plan.setFullyResolved(true);
            plan.getNotices().add("No teaching sessions are affected by this absence request.");
            return plan;
        }

        LocalDate absenceDate = request.getAbsenceDate();
        DayOfWeek dayOfWeek = absenceDate.getDayOfWeek();
        List<Teacher> allTeachers = teacherRepository.findAll();

        int maxDaily = getMaxDailyPeriods();
        int maxWeekly = getMaxWeeklyPeriods();

        // Track temporary substitute assignments within this proposal calculation
        Map<Long, Integer> simulatedDailyLoad = new HashMap<>();
        Map<Long, Integer> simulatedWeeklyLoad = new HashMap<>();

        for (Teacher t : allTeachers) {
            int currentDaily = countPublishedDailyPeriods(t, dayOfWeek);
            int currentWeekly = countPublishedWeeklyPeriods(t);
            simulatedDailyLoad.put(t.getId(), currentDaily);
            simulatedWeeklyLoad.put(t.getId(), currentWeekly);
        }

        int proposedCount = 0;
        int unresolvedCount = 0;

        for (AbsenceAffectedClass ac : affectedClasses) {
            TimetableEntry entry = ac.getTimetableEntry();
            Period period = ac.getPeriod();
            Subject subject = entry != null ? entry.getSubject() : null;

            CandidateTeacher bestCandidate = null;
            int bestScore = Integer.MIN_VALUE;

            for (Teacher candidate : allTeachers) {
                // Hard Constraint 1: Cannot substitute for oneself
                if (candidate.getId().equals(request.getTeacher().getId())) {
                    continue;
                }

                // Hard Constraint 2: Cannot be on approved leave on that date
                if (isTeacherOnApprovedLeave(candidate, absenceDate)) {
                    continue;
                }

                // Hard Constraint 3: Must be available in availability matrix
                Optional<TeacherAvailability> avail = teacherAvailabilityRepository
                        .findByTeacherAndDayOfWeekAndPeriod(candidate, dayOfWeek, period);
                if (avail.isPresent() && !avail.get().isAvailable()) {
                    continue;
                }

                // Hard Constraint 4: Cannot have a conflicting class at that day & period
                if (isTeacherBusy(candidate, dayOfWeek, period)) {
                    continue;
                }

                // Hard Constraint 5: Workload limits
                int dailyLoad = simulatedDailyLoad.getOrDefault(candidate.getId(), 0);
                int weeklyLoad = simulatedWeeklyLoad.getOrDefault(candidate.getId(), 0);
                if (dailyLoad >= maxDaily || weeklyLoad >= maxWeekly) {
                    continue;
                }

                // Hard Constraint 6: Qualification & eligibility
                boolean isQualified = isTeacherEligibleForSubject(candidate, subject);
                if (!isQualified) {
                    continue;
                }

                // Soft Constraints & Fairness Ranking:
                int score = 0;
                // Direct subject match bonus
                if (teachesSameSubject(candidate, subject)) {
                    score += 50;
                }
                // Same department bonus
                if (sharesDepartmentWithSubject(candidate, subject)) {
                    score += 30;
                }
                // Suggested by teacher bonus
                if (request.getSuggestedSubstitute() != null &&
                        request.getSuggestedSubstitute().getId().equals(candidate.getId())) {
                    score += 40;
                }
                // Balanced workload bonus: lower weekly teaching load gets higher priority
                score += (maxWeekly - weeklyLoad);

                if (score > bestScore) {
                    bestScore = score;
                    bestCandidate = new CandidateTeacher(candidate, score, weeklyLoad);
                }
            }

            if (bestCandidate != null) {
                Teacher selected = bestCandidate.teacher;
                ac.setProposedSubstitute(selected);
                ac.setProposalStatus("PROPOSED");

                String deptName = selected.getDepartments() != null && !selected.getDepartments().isEmpty()
                        ? selected.getDepartments().iterator().next().getCode() : "Dept";
                String reason = "Qualified (" + deptName + "), verified zero timetable conflicts. Weekly load: "
                        + bestCandidate.weeklyLoad + "/" + maxWeekly + " periods.";
                ac.setProposalReason(reason);

                // Update simulated load so subsequent slots in the same day account for this assignment
                simulatedDailyLoad.put(selected.getId(), simulatedDailyLoad.get(selected.getId()) + 1);
                simulatedWeeklyLoad.put(selected.getId(), simulatedWeeklyLoad.get(selected.getId()) + 1);
                proposedCount++;
            } else {
                ac.setProposedSubstitute(null);
                ac.setProposalStatus("UNRESOLVED");
                ac.setProposalReason("No eligible faculty member available for Period "
                        + period.getPeriodNumber() + " within daily/weekly workload limits.");
                unresolvedCount++;
            }
        }

        absenceAffectedClassRepository.saveAll(affectedClasses);
        plan.setProposedCount(proposedCount);
        plan.setUnresolvedCount(unresolvedCount);
        plan.setFullyResolved(unresolvedCount == 0);
        plan.setAffectedClasses(affectedClasses);

        if (unresolvedCount > 0) {
            plan.getNotices().add("Warning: " + unresolvedCount + " slot(s) could not be automatically resolved. Manual administrative assignment or class cancellation is required.");
        } else {
            plan.getNotices().add("All " + proposedCount + " affected class(es) successfully mapped to qualified substitute faculty.");
        }

        // Call Ollama for high-level plan explanation if available
        try {
            String aiSummary = ollamaService.explainReplacementProposal(
                    request.getTeacher(),
                    proposedCount > 0 && affectedClasses.get(0).getProposedSubstitute() != null
                            ? affectedClasses.get(0).getProposedSubstitute() : null,
                    affectedClasses.get(0).getTimetableEntry(),
                    plan.getNotices().get(0)
            );
            plan.setAiSummary(aiSummary);
        } catch (Exception ignored) {}

        return plan;
    }

    private boolean isTeacherOnApprovedLeave(Teacher teacher, LocalDate date) {
        List<AbsenceRequest> approved = absenceRequestRepository.findByTeacherAndAbsenceDate(teacher, date);
        return approved.stream().anyMatch(r -> r.getStatus() == RequestStatus.APPROVED);
    }

    private boolean isTeacherBusy(Teacher teacher, DayOfWeek day, Period period) {
        List<TimetableEntry> entries = timetableEntryRepository
                .findByTeacherAndDayOfWeekAndPeriodAndPublishedTrue(teacher, day, period);
        for (TimetableEntry e : entries) {
            if (e.getStatus() != EntryStatus.CANCELLED) {
                return true;
            }
        }
        return false;
    }

    private int countPublishedDailyPeriods(Teacher teacher, DayOfWeek day) {
        List<TimetableEntry> entries = timetableEntryRepository.findByTeacherAndDayOfWeekAndPublished(teacher, day, true);
        return (int) entries.stream().filter(e -> e.getStatus() != EntryStatus.CANCELLED).count();
    }

    private int countPublishedWeeklyPeriods(Teacher teacher) {
        List<TimetableEntry> entries = timetableEntryRepository.findByTeacherAndPublished(teacher, true);
        return (int) entries.stream().filter(e -> e.getStatus() != EntryStatus.CANCELLED).count();
    }

    private boolean isTeacherEligibleForSubject(Teacher teacher, Subject subject) {
        if (subject == null) return true;
        // Direct qualification: teacher is assigned to this subject in any section
        if (teachesSameSubject(teacher, subject)) {
            return true;
        }
        // Department qualification: teacher belongs to subject's department
        return sharesDepartmentWithSubject(teacher, subject);
    }

    private boolean teachesSameSubject(Teacher teacher, Subject subject) {
        if (subject == null) return false;
        List<SubjectAssignment> assignments = subjectAssignmentRepository.findByTeacher(teacher);
        for (SubjectAssignment sa : assignments) {
            if (sa.getSubject().getId().equals(subject.getId()) ||
                    sa.getSubject().getCode().equalsIgnoreCase(subject.getCode())) {
                return true;
            }
        }
        return false;
    }

    private boolean sharesDepartmentWithSubject(Teacher teacher, Subject subject) {
        if (subject == null || subject.getDepartment() == null || teacher.getDepartments() == null) {
            return false;
        }
        Long subDeptId = subject.getDepartment().getId();
        for (Department d : teacher.getDepartments()) {
            if (d.getId().equals(subDeptId)) {
                return true;
            }
        }
        return false;
    }

    private static class CandidateTeacher {
        final Teacher teacher;
        final int score;
        final int weeklyLoad;

        CandidateTeacher(Teacher teacher, int score, int weeklyLoad) {
            this.teacher = teacher;
            this.score = score;
            this.weeklyLoad = weeklyLoad;
        }
    }
}
