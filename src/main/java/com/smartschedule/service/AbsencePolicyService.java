package com.smartschedule.service;

import com.smartschedule.dto.PolicyValidationResult;
import com.smartschedule.model.AbsenceAffectedClass;
import com.smartschedule.model.AbsenceRequest;
import com.smartschedule.model.Teacher;
import com.smartschedule.model.enums.AbsenceType;
import com.smartschedule.model.enums.RequestStatus;
import com.smartschedule.repository.AbsenceRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AbsencePolicyService {

    private final AbsenceRequestRepository absenceRequestRepository;
    private final SystemSettingsService systemSettingsService;

    private static final List<RequestStatus> ACTIVE_STATUSES = Arrays.asList(
            RequestStatus.PENDING,
            RequestStatus.APPROVED
    );

    public AbsencePolicyService(AbsenceRequestRepository absenceRequestRepository,
                                SystemSettingsService systemSettingsService) {
        this.absenceRequestRepository = absenceRequestRepository;
        this.systemSettingsService = systemSettingsService;
    }

    public int getWeeklyLimit() {
        try {
            return Integer.parseInt(systemSettingsService.getSetting("max_weekly_absence_requests", "2"));
        } catch (NumberFormatException e) {
            return 2;
        }
    }

    public int getMonthlyLimit() {
        try {
            return Integer.parseInt(systemSettingsService.getSetting("max_monthly_leave_days", "7"));
        } catch (NumberFormatException e) {
            return 7;
        }
    }

    public PolicyValidationResult validateAbsenceRequest(Teacher teacher,
                                                         LocalDate date,
                                                         AbsenceType type,
                                                         List<Long> periodIds,
                                                         Long excludeRequestId) {
        if (teacher == null || date == null || type == null) {
            return PolicyValidationResult.duplicate("Invalid request parameters.");
        }

        // 1. Check for duplicate or overlapping requests on the same date
        List<AbsenceRequest> sameDayRequests = absenceRequestRepository.findByTeacherAndAbsenceDate(teacher, date);
        for (AbsenceRequest req : sameDayRequests) {
            if (excludeRequestId != null && req.getId().equals(excludeRequestId)) {
                continue;
            }
            if (!ACTIVE_STATUSES.contains(req.getStatus())) {
                continue; // Ignore rejected or withdrawn requests
            }

            // If either request is FULL_DAY, or both are on the same date with overlapping periods
            if (req.getAbsenceType() == AbsenceType.FULL_DAY || type == AbsenceType.FULL_DAY) {
                return PolicyValidationResult.duplicate(
                        "An active absence request already exists for " + date + " (Status: " + req.getStatus() + "). Duplicate or overlapping leave is not permitted.");
            }

            // Both are SPECIFIC_PERIODS -> check if periods overlap
            if (req.getAffectedClasses() != null && periodIds != null && !periodIds.isEmpty()) {
                Set<Long> existingPeriods = req.getAffectedClasses().stream()
                        .map(ac -> ac.getPeriod().getId())
                        .collect(Collectors.toSet());
                for (Long pid : periodIds) {
                    if (existingPeriods.contains(pid)) {
                        return PolicyValidationResult.duplicate(
                                "An active request already exists covering period ID " + pid + " on " + date + ". Overlapping periods are not allowed.");
                    }
                }
            }
        }

        // 2. Weekly limit (Monday through Sunday)
        LocalDate monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sunday = date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        List<AbsenceRequest> weeklyRequests = absenceRequestRepository
                .findByTeacherAndAbsenceDateBetweenAndStatusIn(teacher, monday, sunday, ACTIVE_STATUSES);

        long activeWeeklySubmissions = weeklyRequests.stream()
                .filter(r -> excludeRequestId == null || !r.getId().equals(excludeRequestId))
                .count();

        int proposedWeeklyCount = (int) activeWeeklySubmissions + 1;
        int weeklyLimit = getWeeklyLimit();

        // 3. Monthly limit (Calendar month)
        LocalDate monthStart = date.withDayOfMonth(1);
        LocalDate monthEnd = date.withDayOfMonth(date.lengthOfMonth());

        List<AbsenceRequest> monthlyRequests = absenceRequestRepository
                .findByTeacherAndAbsenceDateBetweenAndStatusIn(teacher, monthStart, monthEnd, ACTIVE_STATUSES);

        double activeMonthlyDays = monthlyRequests.stream()
                .filter(r -> excludeRequestId == null || !r.getId().equals(excludeRequestId))
                .mapToDouble(r -> r.getAbsenceType() == AbsenceType.FULL_DAY ? 1.0 : 0.5)
                .sum();

        double proposedLeaveDuration = (type == AbsenceType.FULL_DAY ? 1.0 : 0.5);
        double proposedMonthlyDays = activeMonthlyDays + proposedLeaveDuration;
        int monthlyLimit = getMonthlyLimit();

        // 4. Policy limit checks
        boolean weeklyExceeded = proposedWeeklyCount > weeklyLimit;
        boolean monthlyExceeded = proposedMonthlyDays > monthlyLimit;

        if (weeklyExceeded || monthlyExceeded) {
            StringBuilder sb = new StringBuilder("POLICY VIOLATION: ");
            if (weeklyExceeded) {
                sb.append("Weekly request limit exceeded (").append(proposedWeeklyCount)
                        .append("/").append(weeklyLimit).append(" for week ").append(monday).append(" to ").append(sunday).append("). ");
            }
            if (monthlyExceeded) {
                sb.append("Monthly leave limit exceeded (").append(proposedMonthlyDays)
                        .append("/").append(monthlyLimit).append(" days for ").append(date.getMonth()).append(" ").append(date.getYear()).append(").");
            }
            return PolicyValidationResult.violation(sb.toString().trim(), proposedWeeklyCount, weeklyLimit, proposedMonthlyDays, monthlyLimit);
        }

        return PolicyValidationResult.ok(proposedWeeklyCount, weeklyLimit, proposedMonthlyDays, monthlyLimit);
    }

    public int getTeacherWeeklyRequestCount(Teacher teacher, LocalDate date) {
        LocalDate monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sunday = date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        List<AbsenceRequest> list = absenceRequestRepository
                .findByTeacherAndAbsenceDateBetweenAndStatusIn(teacher, monday, sunday, ACTIVE_STATUSES);
        return list.size();
    }

    public double getTeacherMonthlyLeaveDays(Teacher teacher, LocalDate date) {
        LocalDate monthStart = date.withDayOfMonth(1);
        LocalDate monthEnd = date.withDayOfMonth(date.lengthOfMonth());
        List<AbsenceRequest> list = absenceRequestRepository
                .findByTeacherAndAbsenceDateBetweenAndStatusIn(teacher, monthStart, monthEnd, ACTIVE_STATUSES);
        return list.stream()
                .mapToDouble(r -> r.getAbsenceType() == AbsenceType.FULL_DAY ? 1.0 : 0.5)
                .sum();
    }
}
