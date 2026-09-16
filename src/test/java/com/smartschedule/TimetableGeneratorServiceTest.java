package com.smartschedule;

import com.smartschedule.dto.GenerationResult;
import com.smartschedule.model.*;
import com.smartschedule.model.enums.*;
import com.smartschedule.repository.*;
import com.smartschedule.service.TimetableGeneratorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimetableGeneratorServiceTest {

    @Mock
    private TimetableEntryRepository timetableEntryRepository;

    @Mock
    private SubjectAssignmentRepository subjectAssignmentRepository;

    @Mock
    private PeriodRepository periodRepository;

    @Mock
    private ClassroomRepository classroomRepository;

    @Mock
    private TeacherAvailabilityRepository teacherAvailabilityRepository;

    @InjectMocks
    private TimetableGeneratorService timetableGeneratorService;

    private Section section;
    private Department department;
    private AcademicYear academicYear;
    private List<Period> periods;
    private List<Classroom> classrooms;
    private List<SubjectAssignment> assignments;

    @BeforeEach
    void setUp() {
        department = new Department();
        department.setId(1L);
        department.setCode("CSE");
        department.setName("Computer Science");

        academicYear = new AcademicYear();
        academicYear.setId(1L);
        academicYear.setYearNumber(2);
        academicYear.setLabel("2nd Year");

        section = new Section();
        section.setId(1L);
        section.setName("A");
        section.setDisplayName("CSE-A");
        section.setDepartment(department);
        section.setAcademicYear(academicYear);

        periods = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            Period p = new Period();
            p.setId((long) i);
            p.setPeriodNumber(i);
            p.setType(PeriodType.CLASS);
            p.setStartTime(LocalTime.of(8 + i, 0));
            p.setEndTime(LocalTime.of(8 + i, 50));
            periods.add(p);
        }

        classrooms = new ArrayList<>();
        Classroom c1 = new Classroom();
        c1.setId(1L);
        c1.setName("Room 101");
        c1.setRoomType(RoomType.CLASSROOM);
        c1.setCapacity(60);
        classrooms.add(c1);

        Classroom c2 = new Classroom();
        c2.setId(2L);
        c2.setName("Lab 1");
        c2.setRoomType(RoomType.COMPUTER_LAB);
        c2.setCapacity(40);
        classrooms.add(c2);

        Subject sub1 = new Subject();
        sub1.setId(1L);
        sub1.setCode("CS201");
        sub1.setName("Data Structures");
        sub1.setType(SubjectType.THEORY);
        sub1.setPeriodsPerWeek(4);

        User u1 = new User();
        u1.setId(2L);
        u1.setFullName("Dr. Alan Turing");

        Teacher t1 = new Teacher();
        t1.setId(1L);
        t1.setTeacherId("T101");
        t1.setUser(u1);

        SubjectAssignment sa1 = new SubjectAssignment();
        sa1.setId(1L);
        sa1.setSubject(sub1);
        sa1.setTeacher(t1);
        sa1.setSection(section);

        assignments = Collections.singletonList(sa1);
    }

    @Test
    void testGenerateTimetableSuccess() {
        when(subjectAssignmentRepository.findBySection(section)).thenReturn(assignments);
        when(periodRepository.findByTypeOrderByPeriodNumberAsc(PeriodType.CLASS)).thenReturn(periods);
        when(timetableEntryRepository.findBySectionAndLockedTrue(section)).thenReturn(new ArrayList<>());
        when(classroomRepository.findAll()).thenReturn(classrooms);
        when(timetableEntryRepository.findByDayOfWeekAndPeriod(any(), any())).thenReturn(new ArrayList<>());
        when(timetableEntryRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));

        GenerationResult result = timetableGeneratorService.generateAndValidate(section);

        assertNotNull(result);
        assertEquals(1, result.getSectionsProcessed());
        assertEquals(4, result.getClassesScheduled());
        assertEquals(0, result.getConflicts());
    }
}
