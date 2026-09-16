package com.smartschedule;

import com.smartschedule.dto.AbsenceRequestDTO;
import com.smartschedule.model.*;
import com.smartschedule.model.enums.*;
import com.smartschedule.repository.*;
import com.smartschedule.service.AbsenceService;
import com.smartschedule.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AbsenceServiceTest {

    @Mock
    private AbsenceRequestRepository absenceRequestRepository;

    @Mock
    private AbsenceAffectedClassRepository absenceAffectedClassRepository;

    @Mock
    private TimetableEntryRepository timetableEntryRepository;

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private PeriodRepository periodRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private AbsenceService absenceService;

    private Teacher teacher;
    private User teacherUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        teacherUser = new User();
        teacherUser.setId(2L);
        teacherUser.setFullName("Dr. Alan Turing");

        teacher = new Teacher();
        teacher.setId(1L);
        teacher.setTeacherId("T101");
        teacher.setUser(teacherUser);

        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setFullName("System Administrator");
        adminUser.setRole(UserRole.ROLE_ADMIN);
    }

    @Test
    void testSubmitAbsenceFullDay() {
        AbsenceRequestDTO dto = new AbsenceRequestDTO();
        dto.setAbsenceDate(LocalDate.now().plusDays(1));
        dto.setAbsenceType(AbsenceType.FULL_DAY);
        dto.setReason("Medical Checkup");

        TimetableEntry entry = new TimetableEntry();
        entry.setId(10L);
        entry.setDayOfWeek(dto.getAbsenceDate().getDayOfWeek());
        entry.setTeacher(teacher);
        entry.setPublished(true);
        entry.setStatus(EntryStatus.ACTIVE);

        Period p = new Period();
        p.setId(1L);
        p.setPeriodNumber(1);
        p.setType(PeriodType.CLASS);
        entry.setPeriod(p);

        when(absenceRequestRepository.save(any())).thenAnswer(i -> {
            AbsenceRequest r = i.getArgument(0);
            r.setId(100L);
            return r;
        });
        when(timetableEntryRepository.findByTeacherAndDayOfWeekAndPublishedTrue(teacher, dto.getAbsenceDate().getDayOfWeek()))
                .thenReturn(Collections.singletonList(entry));

        AbsenceRequest saved = absenceService.submitAbsence(dto, teacher);

        assertNotNull(saved);
        assertEquals(RequestStatus.PENDING, saved.getStatus());
        verify(absenceAffectedClassRepository, times(1)).save(any());
    }

    @Test
    void testApproveAbsence() {
        AbsenceRequest req = new AbsenceRequest();
        req.setId(100L);
        req.setTeacher(teacher);
        req.setStatus(RequestStatus.PENDING);
        req.setAbsenceDate(LocalDate.now().plusDays(1));

        when(absenceRequestRepository.findById(100L)).thenReturn(Optional.of(req));
        when(absenceRequestRepository.save(any())).thenReturn(req);

        AbsenceRequest approved = absenceService.approveAbsence(100L, "Approved by Principal", adminUser);

        assertEquals(RequestStatus.APPROVED, approved.getStatus());
        assertEquals("Approved by Principal", approved.getAdminComment());
        verify(notificationService, times(1)).createNotification(eq(teacherUser), any(), any(), eq(NotificationType.ABSENCE_APPROVED));
    }
}
