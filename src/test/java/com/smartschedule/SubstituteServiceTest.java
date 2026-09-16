package com.smartschedule;

import com.smartschedule.model.*;
import com.smartschedule.model.enums.*;
import com.smartschedule.repository.*;
import com.smartschedule.service.NotificationService;
import com.smartschedule.service.SubstituteService;
import com.smartschedule.service.TeacherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubstituteServiceTest {

    @Mock
    private SubstituteAssignmentRepository substituteAssignmentRepository;

    @Mock
    private AbsenceAffectedClassRepository absenceAffectedClassRepository;

    @Mock
    private TimetableEntryRepository timetableEntryRepository;

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private TeacherService teacherService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private SubstituteService substituteService;

    private User adminUser;
    private Teacher absentTeacher;
    private Teacher subTeacher;
    private AbsenceAffectedClass affectedClass;
    private TimetableEntry entry;
    private Section section;

    @BeforeEach
    void setUp() {
        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setFullName("Admin");

        User u1 = new User();
        u1.setId(2L);
        u1.setFullName("Dr. Alan Turing");
        absentTeacher = new Teacher();
        absentTeacher.setId(1L);
        absentTeacher.setUser(u1);

        User u2 = new User();
        u2.setId(3L);
        u2.setFullName("Prof. Grace Hopper");
        subTeacher = new Teacher();
        subTeacher.setId(2L);
        subTeacher.setUser(u2);

        section = new Section();
        section.setId(1L);
        section.setDisplayName("CSE-A");

        entry = new TimetableEntry();
        entry.setId(10L);
        entry.setTeacher(absentTeacher);
        entry.setSection(section);
        entry.setStatus(EntryStatus.ACTIVE);

        Period p = new Period();
        p.setId(1L);
        p.setPeriodNumber(1);
        entry.setPeriod(p);

        AbsenceRequest req = new AbsenceRequest();
        req.setId(50L);
        req.setTeacher(absentTeacher);
        req.setAbsenceDate(LocalDate.now().plusDays(1));

        affectedClass = new AbsenceAffectedClass();
        affectedClass.setId(100L);
        affectedClass.setAbsenceRequest(req);
        affectedClass.setTimetableEntry(entry);
        affectedClass.setPeriod(p);
        affectedClass.setResolution(ClassResolution.PENDING);
    }

    @Test
    void testAssignSubstitute() {
        when(absenceAffectedClassRepository.findById(100L)).thenReturn(Optional.of(affectedClass));
        when(teacherRepository.findById(2L)).thenReturn(Optional.of(subTeacher));
        when(substituteAssignmentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        SubstituteAssignment sa = substituteService.assignSubstitute(100L, 2L, adminUser);

        assertNotNull(sa);
        assertEquals(ClassResolution.SUBSTITUTE_ASSIGNED, affectedClass.getResolution());
        assertEquals(EntryStatus.SUBSTITUTE, entry.getStatus());
        assertEquals(subTeacher, entry.getSubstituteTeacher());
        verify(notificationService, times(1)).notifyTeacher(eq(subTeacher), any(), any(), eq(NotificationType.SUBSTITUTE_ASSIGNED));
        verify(notificationService, times(1)).notifyStudentsInSection(eq(section), any(), any(), eq(NotificationType.SUBSTITUTE_ASSIGNED));
    }

    @Test
    void testCancelClass() {
        when(absenceAffectedClassRepository.findById(100L)).thenReturn(Optional.of(affectedClass));

        substituteService.cancelClass(100L, adminUser);

        assertEquals(ClassResolution.CANCELLED, affectedClass.getResolution());
        assertEquals(EntryStatus.CANCELLED, entry.getStatus());
        verify(notificationService, times(1)).notifyStudentsInSection(eq(section), any(), any(), eq(NotificationType.CLASS_CANCELLATION));
    }
}
