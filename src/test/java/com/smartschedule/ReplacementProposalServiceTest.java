package com.smartschedule;

import com.smartschedule.dto.ReplacementPlanDTO;
import com.smartschedule.model.*;
import com.smartschedule.model.enums.AbsenceType;
import com.smartschedule.model.enums.EntryStatus;
import com.smartschedule.model.enums.PeriodType;
import com.smartschedule.model.enums.RequestStatus;
import com.smartschedule.repository.*;
import com.smartschedule.service.OllamaService;
import com.smartschedule.service.ReplacementProposalService;
import com.smartschedule.service.SystemSettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReplacementProposalServiceTest {

    @Mock
    private AbsenceAffectedClassRepository absenceAffectedClassRepository;

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private TimetableEntryRepository timetableEntryRepository;

    @Mock
    private TeacherAvailabilityRepository teacherAvailabilityRepository;

    @Mock
    private AbsenceRequestRepository absenceRequestRepository;

    @Mock
    private SubjectAssignmentRepository subjectAssignmentRepository;

    @Mock
    private SystemSettingsService systemSettingsService;

    @Mock
    private OllamaService ollamaService;

    @InjectMocks
    private ReplacementProposalService replacementProposalService;

    private Teacher absentTeacher;
    private Teacher candidateTeacher;
    private Subject subject;
    private AbsenceRequest request;
    private AbsenceAffectedClass affectedClass;

    @BeforeEach
    void setUp() {
        Department dept = new Department();
        dept.setId(1L);
        dept.setCode("CSE");

        absentTeacher = new Teacher();
        absentTeacher.setId(1L);
        User u1 = new User();
        u1.setFullName("Dr. Alan Turing");
        absentTeacher.setUser(u1);
        absentTeacher.setDepartments(Set.of(dept));

        candidateTeacher = new Teacher();
        candidateTeacher.setId(2L);
        User u2 = new User();
        u2.setFullName("Prof. Grace Hopper");
        candidateTeacher.setUser(u2);
        candidateTeacher.setDepartments(Set.of(dept));

        subject = new Subject();
        subject.setId(10L);
        subject.setName("Data Structures");
        subject.setCode("CS201");
        subject.setDepartment(dept);

        request = new AbsenceRequest();
        request.setId(100L);
        request.setTeacher(absentTeacher);
        request.setAbsenceDate(LocalDate.of(2026, 11, 4)); // Wednesday
        request.setAbsenceType(AbsenceType.FULL_DAY);

        Period period = new Period();
        period.setId(1L);
        period.setPeriodNumber(1);
        period.setType(PeriodType.CLASS);

        Section section = new Section();
        section.setId(1L);
        section.setDisplayName("CSE-A");

        TimetableEntry entry = new TimetableEntry();
        entry.setId(500L);
        entry.setSection(section);
        entry.setDayOfWeek(DayOfWeek.WEDNESDAY);
        entry.setPeriod(period);
        entry.setSubject(subject);
        entry.setTeacher(absentTeacher);

        affectedClass = new AbsenceAffectedClass();
        affectedClass.setId(10L);
        affectedClass.setAbsenceRequest(request);
        affectedClass.setPeriod(period);
        affectedClass.setTimetableEntry(entry);

        when(absenceAffectedClassRepository.findByAbsenceRequest(request))
                .thenReturn(Collections.singletonList(affectedClass));
        when(teacherRepository.findAll()).thenReturn(List.of(absentTeacher, candidateTeacher));
    }

    @Test
    void testAutomaticReplacement_WhenEligibleCandidateAvailable() {
        // Candidate has 0 conflicts, is available, within workload
        when(timetableEntryRepository.findByTeacherAndDayOfWeekAndPeriodAndPublishedTrue(eq(candidateTeacher), any(), any()))
                .thenReturn(Collections.emptyList());
        when(teacherAvailabilityRepository.findByTeacherAndDayOfWeekAndPeriod(eq(candidateTeacher), any(), any()))
                .thenReturn(Optional.empty());

        ReplacementPlanDTO plan = replacementProposalService.calculateReplacementPlan(request);

        assertNotNull(plan);
        assertEquals(1, plan.getProposedCount());
        assertEquals(0, plan.getUnresolvedCount());
        assertTrue(plan.isFullyResolved());
        assertEquals(candidateTeacher, affectedClass.getProposedSubstitute());
        assertEquals("PROPOSED", affectedClass.getProposalStatus());
        assertTrue(affectedClass.getProposalReason().contains("Qualified"));
    }

    @Test
    void testUnresolved_WhenCandidateHasTimetableConflict() {
        // Candidate is already busy teaching another section at that period
        TimetableEntry conflictingEntry = new TimetableEntry();
        conflictingEntry.setStatus(EntryStatus.ACTIVE);

        when(timetableEntryRepository.findByTeacherAndDayOfWeekAndPeriodAndPublishedTrue(eq(candidateTeacher), any(), any()))
                .thenReturn(Collections.singletonList(conflictingEntry));

        ReplacementPlanDTO plan = replacementProposalService.calculateReplacementPlan(request);

        assertNotNull(plan);
        assertEquals(0, plan.getProposedCount());
        assertEquals(1, plan.getUnresolvedCount());
        assertFalse(plan.isFullyResolved());
        assertNull(affectedClass.getProposedSubstitute());
        assertEquals("UNRESOLVED", affectedClass.getProposalStatus());
    }
}
