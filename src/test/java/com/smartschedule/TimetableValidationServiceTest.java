package com.smartschedule;

import com.smartschedule.model.*;
import com.smartschedule.model.enums.EntryStatus;
import com.smartschedule.model.enums.PeriodType;
import com.smartschedule.model.enums.RoomType;
import com.smartschedule.model.enums.SubjectType;
import com.smartschedule.repository.SubjectAssignmentRepository;
import com.smartschedule.repository.TeacherAvailabilityRepository;
import com.smartschedule.repository.TimetableEntryRepository;
import com.smartschedule.service.SystemSettingsService;
import com.smartschedule.service.TimetableValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimetableValidationServiceTest {

    @Mock
    private SubjectAssignmentRepository subjectAssignmentRepository;

    @Mock
    private TimetableEntryRepository timetableEntryRepository;

    @Mock
    private TeacherAvailabilityRepository teacherAvailabilityRepository;

    @Mock
    private SystemSettingsService systemSettingsService;

    @InjectMocks
    private TimetableValidationService timetableValidationService;

    private Section section;
    private Teacher teacher;
    private Classroom room;
    private Period period;
    private Subject theorySubject;
    private Subject labSubject;

    @BeforeEach
    void setUp() {
        section = new Section();
        section.setId(1L);
        section.setDisplayName("CSE-A");

        teacher = new Teacher();
        teacher.setId(10L);
        User u = new User();
        u.setFullName("Dr. Alan Turing");
        teacher.setUser(u);

        room = new Classroom();
        room.setId(100L);
        room.setName("Room 101");
        room.setRoomType(RoomType.CLASSROOM);

        period = new Period();
        period.setId(1L);
        period.setPeriodNumber(1);
        period.setType(PeriodType.CLASS);

        theorySubject = new Subject();
        theorySubject.setId(1L);
        theorySubject.setName("Operating Systems");
        theorySubject.setType(SubjectType.THEORY);

        labSubject = new Subject();
        labSubject.setId(2L);
        labSubject.setName("OS Lab");
        labSubject.setType(SubjectType.LAB);
    }

    @Test
    void testValidTimetable_PassesAllConstraints() {
        TimetableEntry e = new TimetableEntry();
        e.setSection(section);
        e.setDayOfWeek(DayOfWeek.MONDAY);
        e.setPeriod(period);
        e.setSubject(theorySubject);
        e.setTeacher(teacher);
        e.setClassroom(room);

        List<String> violations = timetableValidationService.validateHardConstraints(List.of(e), section);
        assertTrue(violations.isEmpty(), "Expected 0 violations for valid entry");
    }

    @Test
    void testTeacherDoubleBooking_IsDetected() {
        // Two entries in the candidate timetable with same teacher at Monday Period 1
        Section secB = new Section();
        secB.setId(2L);
        secB.setDisplayName("CSE-B");

        TimetableEntry e1 = new TimetableEntry();
        e1.setSection(section);
        e1.setDayOfWeek(DayOfWeek.MONDAY);
        e1.setPeriod(period);
        e1.setSubject(theorySubject);
        e1.setTeacher(teacher);
        e1.setClassroom(room);

        TimetableEntry e2 = new TimetableEntry();
        e2.setSection(secB);
        e2.setDayOfWeek(DayOfWeek.MONDAY);
        e2.setPeriod(period);
        e2.setSubject(theorySubject);
        e2.setTeacher(teacher);
        e2.setClassroom(room);

        List<String> violations = timetableValidationService.validateHardConstraints(List.of(e1, e2), section);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.contains("double-booked")));
    }

    @Test
    void testRoomCapabilityMismatch_IsDetected() {
        // Lab scheduled in standard CLASSROOM
        TimetableEntry e = new TimetableEntry();
        e.setSection(section);
        e.setDayOfWeek(DayOfWeek.TUESDAY);
        e.setPeriod(period);
        e.setSubject(labSubject);
        e.setTeacher(teacher);
        e.setClassroom(room); // Standard classroom, not a lab

        List<String> violations = timetableValidationService.validateHardConstraints(List.of(e), section);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.contains("Capability Mismatch")));
    }

    @Test
    void testTeacherUnavailability_IsDetected() {
        TeacherAvailability unavail = new TeacherAvailability();
        unavail.setAvailable(false);

        when(teacherAvailabilityRepository.findByTeacherAndDayOfWeekAndPeriod(eq(teacher), eq(DayOfWeek.WEDNESDAY), eq(period)))
                .thenReturn(Optional.of(unavail));

        TimetableEntry e = new TimetableEntry();
        e.setSection(section);
        e.setDayOfWeek(DayOfWeek.WEDNESDAY);
        e.setPeriod(period);
        e.setSubject(theorySubject);
        e.setTeacher(teacher);
        e.setClassroom(room);

        List<String> violations = timetableValidationService.validateHardConstraints(List.of(e), section);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.contains("Availability Violation")));
    }
}
