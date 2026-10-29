package com.smartschedule;

import com.smartschedule.dto.PolicyValidationResult;
import com.smartschedule.model.AbsenceRequest;
import com.smartschedule.model.Teacher;
import com.smartschedule.model.enums.AbsenceType;
import com.smartschedule.model.enums.RequestStatus;
import com.smartschedule.repository.AbsenceRequestRepository;
import com.smartschedule.service.AbsencePolicyService;
import com.smartschedule.service.SystemSettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AbsencePolicyServiceTest {

    @Mock
    private AbsenceRequestRepository absenceRequestRepository;

    @Mock
    private SystemSettingsService systemSettingsService;

    @InjectMocks
    private AbsencePolicyService absencePolicyService;

    private Teacher teacher;

    @BeforeEach
    void setUp() {
        teacher = new Teacher();
        teacher.setId(10L);
        teacher.setTeacherId("T101");

        when(systemSettingsService.getSetting(eq("max_weekly_absence_requests"), anyString())).thenReturn("2");
        when(systemSettingsService.getSetting(eq("max_monthly_leave_days"), anyString())).thenReturn("7");
    }

    @Test
    void testRequestWithinLimits_ReturnsOk() {
        LocalDate thursday = LocalDate.of(2026, 11, 5);

        when(absenceRequestRepository.findByTeacherAndAbsenceDate(eq(teacher), eq(thursday)))
                .thenReturn(Collections.emptyList());
        when(absenceRequestRepository.findByTeacherAndAbsenceDateBetweenAndStatusIn(eq(teacher), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        PolicyValidationResult result = absencePolicyService.validateAbsenceRequest(
                teacher, thursday, AbsenceType.FULL_DAY, null, null);

        assertTrue(result.isValid());
        assertFalse(result.isViolated());
        assertFalse(result.isDuplicate());
        assertEquals(1, result.getWeeklyRequestCount());
        assertEquals(2, result.getWeeklyLimit());
        assertEquals(1.0, result.getMonthlyLeaveDays());
        assertEquals(7, result.getMonthlyLimit());
    }

    @Test
    void testThirdRequestInSameWeek_FlagsPolicyViolation() {
        LocalDate monday = LocalDate.of(2026, 11, 2);
        LocalDate wednesday = LocalDate.of(2026, 11, 4);
        LocalDate friday = LocalDate.of(2026, 11, 6);

        AbsenceRequest req1 = new AbsenceRequest();
        req1.setId(1L);
        req1.setAbsenceDate(monday);
        req1.setStatus(RequestStatus.APPROVED);
        req1.setAbsenceType(AbsenceType.FULL_DAY);

        AbsenceRequest req2 = new AbsenceRequest();
        req2.setId(2L);
        req2.setAbsenceDate(wednesday);
        req2.setStatus(RequestStatus.PENDING);
        req2.setAbsenceType(AbsenceType.FULL_DAY);

        List<AbsenceRequest> weeklyExisting = List.of(req1, req2);

        when(absenceRequestRepository.findByTeacherAndAbsenceDate(eq(teacher), eq(friday)))
                .thenReturn(Collections.emptyList());

        LocalDate weekStart = friday.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = friday.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        when(absenceRequestRepository.findByTeacherAndAbsenceDateBetweenAndStatusIn(eq(teacher), eq(weekStart), eq(weekEnd), any()))
                .thenReturn(weeklyExisting);

        LocalDate monthStart = friday.withDayOfMonth(1);
        LocalDate monthEnd = friday.withDayOfMonth(friday.lengthOfMonth());
        when(absenceRequestRepository.findByTeacherAndAbsenceDateBetweenAndStatusIn(eq(teacher), eq(monthStart), eq(monthEnd), any()))
                .thenReturn(weeklyExisting);

        PolicyValidationResult result = absencePolicyService.validateAbsenceRequest(
                teacher, friday, AbsenceType.FULL_DAY, null, null);

        assertTrue(result.isValid());
        assertTrue(result.isViolated());
        assertEquals(3, result.getWeeklyRequestCount());
        assertTrue(result.getMessage().contains("Weekly request limit exceeded"));
    }

    @Test
    void testExceedingMonthlyLeaveDays_FlagsPolicyViolation() {
        LocalDate date = LocalDate.of(2026, 11, 20);

        List<AbsenceRequest> existingMonthRequests = new ArrayList<>();
        for (int i = 1; i <= 7; i++) {
            AbsenceRequest r = new AbsenceRequest();
            r.setId((long) i);
            r.setAbsenceDate(LocalDate.of(2026, 11, i));
            r.setAbsenceType(AbsenceType.FULL_DAY);
            r.setStatus(RequestStatus.APPROVED);
            existingMonthRequests.add(r);
        }

        when(absenceRequestRepository.findByTeacherAndAbsenceDate(eq(teacher), eq(date)))
                .thenReturn(Collections.emptyList());
        when(absenceRequestRepository.findByTeacherAndAbsenceDateBetweenAndStatusIn(eq(teacher), any(), any(), any()))
                .thenReturn(existingMonthRequests);

        PolicyValidationResult result = absencePolicyService.validateAbsenceRequest(
                teacher, date, AbsenceType.FULL_DAY, null, null);

        assertTrue(result.isViolated());
        assertEquals(8.0, result.getMonthlyLeaveDays());
        assertTrue(result.getMessage().contains("Monthly leave limit exceeded"));
    }

    @Test
    void testDuplicateRequestOnSameDate_IsBlocked() {
        LocalDate date = LocalDate.of(2026, 11, 10);

        AbsenceRequest existing = new AbsenceRequest();
        existing.setId(50L);
        existing.setAbsenceDate(date);
        existing.setStatus(RequestStatus.PENDING);
        existing.setAbsenceType(AbsenceType.FULL_DAY);

        when(absenceRequestRepository.findByTeacherAndAbsenceDate(eq(teacher), eq(date)))
                .thenReturn(Collections.singletonList(existing));

        PolicyValidationResult result = absencePolicyService.validateAbsenceRequest(
                teacher, date, AbsenceType.FULL_DAY, null, null);

        assertFalse(result.isValid());
        assertTrue(result.isDuplicate());
        assertTrue(result.getMessage().contains("already exists"));
    }

    @Test
    void testBoundaryCrossing_WeekAndMonthSeparation() {
        // Sunday Nov 08, 2026 vs Monday Nov 09, 2026
        LocalDate sunday = LocalDate.of(2026, 11, 8);
        LocalDate monday = LocalDate.of(2026, 11, 9);

        LocalDate sundayWeekStart = sunday.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate mondayWeekStart = monday.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        assertNotEquals(sundayWeekStart, mondayWeekStart);

        // Month boundary: Oct 31, 2026 vs Nov 01, 2026
        LocalDate oct31 = LocalDate.of(2026, 10, 31);
        LocalDate nov01 = LocalDate.of(2026, 11, 1);
        assertNotEquals(oct31.getMonth(), nov01.getMonth());
    }
}
