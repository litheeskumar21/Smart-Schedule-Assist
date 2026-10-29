package com.smartschedule.config;

import com.smartschedule.model.*;
import com.smartschedule.model.enums.*;
import com.smartschedule.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final AcademicYearRepository academicYearRepository;
    private final SectionRepository sectionRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;
    private final SubjectAssignmentRepository subjectAssignmentRepository;
    private final ClassroomRepository classroomRepository;
    private final PeriodRepository periodRepository;
    private final SystemSettingsRepository systemSettingsRepository;
    private final TimetableEntryRepository timetableEntryRepository;
    private final AnnouncementRepository announcementRepository;
    private final ActivityLogRepository activityLogRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Value("${app.init-data:true}")
    private boolean initData;

    public DataInitializer(UserRepository userRepository,
                           DepartmentRepository departmentRepository,
                           AcademicYearRepository academicYearRepository,
                           SectionRepository sectionRepository,
                           TeacherRepository teacherRepository,
                           StudentRepository studentRepository,
                           SubjectRepository subjectRepository,
                           SubjectAssignmentRepository subjectAssignmentRepository,
                           ClassroomRepository classroomRepository,
                           PeriodRepository periodRepository,
                           SystemSettingsRepository systemSettingsRepository,
                           TimetableEntryRepository timetableEntryRepository,
                           AnnouncementRepository announcementRepository,
                           ActivityLogRepository activityLogRepository,
                           BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.academicYearRepository = academicYearRepository;
        this.sectionRepository = sectionRepository;
        this.teacherRepository = teacherRepository;
        this.studentRepository = studentRepository;
        this.subjectRepository = subjectRepository;
        this.subjectAssignmentRepository = subjectAssignmentRepository;
        this.classroomRepository = classroomRepository;
        this.periodRepository = periodRepository;
        this.systemSettingsRepository = systemSettingsRepository;
        this.timetableEntryRepository = timetableEntryRepository;
        this.announcementRepository = announcementRepository;
        this.activityLogRepository = activityLogRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        ensureSetting("current_semester", "3");
        ensureSetting("academic_year", "2026-2027");
        ensureSetting("max_weekly_absence_requests", "2");
        ensureSetting("max_monthly_leave_days", "7");
        ensureSetting("max_daily_teacher_periods", "4");
        ensureSetting("max_weekly_teacher_periods", "18");

        if (!initData || userRepository.findByUsername("admin").isPresent()) {
            return;
        }
        System.out.println(">>> [SMART SCHEDULE ASSIST] Initializing Database with Sample College Data...");

        // 1. Admin Account
        User admin = new User();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setRole(UserRole.ROLE_ADMIN);
        admin.setFullName("System Administrator");
        admin.setEmail("admin@smartschedule.edu");
        admin.setEnabled(true);
        userRepository.save(admin);

        // 2. System Settings
        createSetting("current_semester", "3");
        createSetting("academic_year", "2026-2027");

        // 3. Departments
        Department cse = createDept("Computer Science and Engineering", "CSE");
        Department ece = createDept("Electronics and Communication Engineering", "ECE");
        Department eee = createDept("Electrical and Electronics Engineering", "EEE");
        Department civil = createDept("Civil Engineering", "Civil");
        Department mech = createDept("Mechanical Engineering", "Mechanical");

        // 4. Academic Years
        AcademicYear yr1 = createYear(1, "1st Year");
        AcademicYear yr2 = createYear(2, "2nd Year");
        AcademicYear yr3 = createYear(3, "3rd Year");
        AcademicYear yr4 = createYear(4, "4th Year");

        // 5. Sections
        Section cseA = createSection("A", cse, yr2);
        Section cseB = createSection("B", cse, yr2);
        Section eceA = createSection("A", ece, yr2);
        Section eceB = createSection("B", ece, yr2);
        Section eeeA = createSection("A", eee, yr2);

        // 6. Periods (6 Academic Classes, 3 Short Breaks, 1 Lunch)
        Period p1 = createPeriod(1, LocalTime.of(9, 0), LocalTime.of(9, 50), PeriodType.CLASS);
        Period b1 = createPeriod(2, LocalTime.of(9, 50), LocalTime.of(10, 0), PeriodType.BREAK);
        Period p2 = createPeriod(3, LocalTime.of(10, 0), LocalTime.of(10, 50), PeriodType.CLASS);
        Period b2 = createPeriod(4, LocalTime.of(10, 50), LocalTime.of(11, 10), PeriodType.BREAK);
        Period p3 = createPeriod(5, LocalTime.of(11, 10), LocalTime.of(12, 0), PeriodType.CLASS);
        Period p4 = createPeriod(6, LocalTime.of(12, 0), LocalTime.of(12, 50), PeriodType.CLASS);
        Period lunch = createPeriod(7, LocalTime.of(12, 50), LocalTime.of(13, 40), PeriodType.LUNCH);
        Period p5 = createPeriod(8, LocalTime.of(13, 40), LocalTime.of(14, 30), PeriodType.CLASS);
        Period b3 = createPeriod(9, LocalTime.of(14, 30), LocalTime.of(14, 40), PeriodType.BREAK);
        Period p6 = createPeriod(10, LocalTime.of(14, 40), LocalTime.of(15, 30), PeriodType.CLASS);

        // 7. Classrooms
        Classroom r101 = createRoom("Room 101", RoomType.CLASSROOM, 60);
        Classroom r102 = createRoom("Room 102", RoomType.CLASSROOM, 60);
        Classroom r103 = createRoom("Room 103", RoomType.CLASSROOM, 60);
        Classroom r104 = createRoom("Room 104", RoomType.CLASSROOM, 60);
        Classroom r105 = createRoom("Room 105", RoomType.CLASSROOM, 40);
        Classroom r201 = createRoom("Room 201", RoomType.CLASSROOM, 60);
        Classroom r202 = createRoom("Room 202", RoomType.CLASSROOM, 60);
        Classroom csLab1 = createRoom("CS Lab 1", RoomType.COMPUTER_LAB, 30);
        Classroom csLab2 = createRoom("CS Lab 2", RoomType.COMPUTER_LAB, 30);
        Classroom ecLab = createRoom("Electronics Lab", RoomType.ELECTRONICS_LAB, 30);
        Classroom phyLab = createRoom("Physics Lab", RoomType.PHYSICS_LAB, 30);
        Classroom semHall = createRoom("Seminar Hall", RoomType.SEMINAR_HALL, 200);

        // 8. Teachers (15)
        Teacher t01 = createTeacher("T001", "Dr. Kumar", "kumar@smartschedule.edu", "9876543210", Set.of(cse));
        Teacher t02 = createTeacher("T002", "Prof. Sharma", "sharma@smartschedule.edu", "9876543211", Set.of(cse));
        Teacher t03 = createTeacher("T003", "Dr. Patel", "patel@smartschedule.edu", "9876543212", Set.of(cse, ece));
        Teacher t04 = createTeacher("T004", "Prof. Reddy", "reddy@smartschedule.edu", "9876543213", Set.of(ece));
        Teacher t05 = createTeacher("T005", "Dr. Singh", "singh@smartschedule.edu", "9876543214", Set.of(ece));
        Teacher t06 = createTeacher("T006", "Prof. Gupta", "gupta@smartschedule.edu", "9876543215", Set.of(eee));
        Teacher t07 = createTeacher("T007", "Dr. Verma", "verma@smartschedule.edu", "9876543216", Set.of(eee));
        Teacher t08 = createTeacher("T008", "Prof. Iyer", "iyer@smartschedule.edu", "9876543217", Set.of(cse));
        Teacher t09 = createTeacher("T009", "Dr. Nair", "nair@smartschedule.edu", "9876543218", Set.of(civil));
        Teacher t10 = createTeacher("T010", "Prof. Das", "das@smartschedule.edu", "9876543219", Set.of(civil));
        Teacher t11 = createTeacher("T011", "Dr. Joshi", "joshi@smartschedule.edu", "9876543220", Set.of(mech));
        Teacher t12 = createTeacher("T012", "Prof. Mehta", "mehta@smartschedule.edu", "9876543221", Set.of(mech));
        Teacher t13 = createTeacher("T013", "Dr. Rao", "rao@smartschedule.edu", "9876543222", Set.of(cse, ece));
        Teacher t14 = createTeacher("T014", "Prof. Pillai", "pillai@smartschedule.edu", "9876543223", Set.of(eee, ece));
        Teacher t15 = createTeacher("T015", "Dr. Bhat", "bhat@smartschedule.edu", "9876543224", Set.of(civil, mech));

        // 9. Students (30 across sections)
        createStudents(1, 6, "S00%d", cse, yr2, cseA, "CSE-A Student");
        createStudents(7, 12, "S00%d", cse, yr2, cseB, "CSE-B Student");
        createStudents(13, 18, "S0%d", ece, yr2, eceA, "ECE-A Student");
        createStudents(19, 24, "S0%d", ece, yr2, eceB, "ECE-B Student");
        createStudents(25, 30, "S0%d", eee, yr2, eeeA, "EEE-A Student");

        // 10. Subjects (CSE 2nd Year)
        Subject ds = createSubject("Data Structures", "CS201", SubjectType.THEORY, 4, cse, yr2);
        Subject dbms = createSubject("Database Management Systems", "CS202", SubjectType.THEORY, 4, cse, yr2);
        Subject oop = createSubject("Object Oriented Programming", "CS203", SubjectType.THEORY, 3, cse, yr2);
        Subject cn = createSubject("Computer Networks", "CS204", SubjectType.THEORY, 3, cse, yr2);
        Subject os = createSubject("Operating Systems", "CS205", SubjectType.THEORY, 4, cse, yr2);
        Subject dsLab = createSubject("Data Structures Lab", "CS206", SubjectType.LAB, 2, cse, yr2);
        Subject dbmsLab = createSubject("DBMS Lab", "CS207", SubjectType.LAB, 2, cse, yr2);
        Subject oopLab = createSubject("OOP Lab", "CS208", SubjectType.LAB, 2, cse, yr2);

        // Subject Assignments for CSE-A
        createAssignment(ds, t01, cseA);
        createAssignment(dbms, t02, cseA);
        createAssignment(oop, t03, cseA);
        createAssignment(cn, t08, cseA);
        createAssignment(os, t13, cseA);
        createAssignment(dsLab, t01, cseA);
        createAssignment(dbmsLab, t02, cseA);
        createAssignment(oopLab, t03, cseA);

        // Subject Assignments for CSE-B
        createAssignment(ds, t02, cseB);
        createAssignment(dbms, t01, cseB);
        createAssignment(oop, t08, cseB);
        createAssignment(cn, t13, cseB);
        createAssignment(os, t03, cseB);
        createAssignment(dsLab, t02, cseB);
        createAssignment(dbmsLab, t01, cseB);
        createAssignment(oopLab, t08, cseB);

        // 11. Published Timetable for CSE-A (2nd Year)
        // Mon-Fri schedule covering all 6 class periods + breaks
        List<Period> allP = List.of(p1, b1, p2, b2, p3, p4, lunch, p5, b3, p6);

        // Monday
        createEntry(cseA, DayOfWeek.MONDAY, p1, ds, t01, r101);
        createEntry(cseA, DayOfWeek.MONDAY, p2, dbms, t02, r101);
        createEntry(cseA, DayOfWeek.MONDAY, p3, oop, t03, r101);
        createEntry(cseA, DayOfWeek.MONDAY, p4, os, t13, r101);
        createEntry(cseA, DayOfWeek.MONDAY, p5, dsLab, t01, csLab1);
        createEntry(cseA, DayOfWeek.MONDAY, p6, dsLab, t01, csLab1);

        // Tuesday
        createEntry(cseA, DayOfWeek.TUESDAY, p1, dbms, t02, r101);
        createEntry(cseA, DayOfWeek.TUESDAY, p2, ds, t01, r101);
        createEntry(cseA, DayOfWeek.TUESDAY, p3, cn, t08, r101);
        createEntry(cseA, DayOfWeek.TUESDAY, p4, os, t13, r101);
        createEntry(cseA, DayOfWeek.TUESDAY, p5, dbmsLab, t02, csLab2);
        createEntry(cseA, DayOfWeek.TUESDAY, p6, dbmsLab, t02, csLab2);

        // Wednesday
        createEntry(cseA, DayOfWeek.WEDNESDAY, p1, oop, t03, r101);
        createEntry(cseA, DayOfWeek.WEDNESDAY, p2, cn, t08, r101);
        createEntry(cseA, DayOfWeek.WEDNESDAY, p3, ds, t01, r101);
        createEntry(cseA, DayOfWeek.WEDNESDAY, p4, dbms, t02, r101);
        createEntry(cseA, DayOfWeek.WEDNESDAY, p5, null, null, null); // Free
        createEntry(cseA, DayOfWeek.WEDNESDAY, p6, null, null, null); // Free

        // Thursday
        createEntry(cseA, DayOfWeek.THURSDAY, p1, os, t13, r101);
        createEntry(cseA, DayOfWeek.THURSDAY, p2, ds, t01, r101);
        createEntry(cseA, DayOfWeek.THURSDAY, p3, oopLab, t03, csLab1);
        createEntry(cseA, DayOfWeek.THURSDAY, p4, oopLab, t03, csLab1);
        createEntry(cseA, DayOfWeek.THURSDAY, p5, cn, t08, r101);
        createEntry(cseA, DayOfWeek.THURSDAY, p6, null, null, null); // Free

        // Friday
        createEntry(cseA, DayOfWeek.FRIDAY, p1, dbms, t02, r101);
        createEntry(cseA, DayOfWeek.FRIDAY, p2, os, t13, r101);
        createEntry(cseA, DayOfWeek.FRIDAY, p3, oop, t03, r101);
        createEntry(cseA, DayOfWeek.FRIDAY, p4, null, null, null); // Free
        createEntry(cseA, DayOfWeek.FRIDAY, p5, null, null, null); // Free
        createEntry(cseA, DayOfWeek.FRIDAY, p6, null, null, null); // Free

        // Add break/lunch entries for all 5 days for CSE-A
        for (DayOfWeek day : List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)) {
            createEntry(cseA, day, b1, null, null, null);
            createEntry(cseA, day, b2, null, null, null);
            createEntry(cseA, day, lunch, null, null, null);
            createEntry(cseA, day, b3, null, null, null);
        }

        // 12. Sample Announcement
        Announcement ann = new Announcement();
        ann.setTitle("Welcome to Semester 3 Academic Year 2026-2027");
        ann.setContent("The academic semester has commenced. Timetables have been published. Please ensure you check your daily schedule, classroom assignments, and 5-minute class reminders.");
        ann.setCreatedBy(admin);
        ann.setTargetType(TargetType.EVERYONE);
        ann.setPublishedAt(LocalDateTime.now().minusHours(2));
        ann.setActive(true);
        announcementRepository.save(ann);

        // 13. Activity Log
        ActivityLog log = new ActivityLog();
        log.setAction("SYSTEM_INIT");
        log.setEntityType("System");
        log.setPerformedBy(admin);
        log.setDetails("Initialized college database with sample departments, sections, subjects, users, and timetable.");
        log.setTimestamp(LocalDateTime.now());
        activityLogRepository.save(log);

        System.out.println(">>> [SMART SCHEDULE ASSIST] Sample Data Initialized Successfully!");
        System.out.println(">>> Admin: admin / admin123");
        System.out.println(">>> Teacher: T001 / teacher123");
        System.out.println(">>> Student: S001 / student123");
    }

    private void createSetting(String key, String val) {
        SystemSettings s = new SystemSettings();
        s.setSettingKey(key);
        s.setSettingValue(val);
        systemSettingsRepository.save(s);
    }

    private void ensureSetting(String key, String defaultVal) {
        if (systemSettingsRepository.findBySettingKey(key).isEmpty()) {
            createSetting(key, defaultVal);
        }
    }

    private Department createDept(String name, String code) {
        Department d = new Department();
        d.setName(name);
        d.setCode(code);
        return departmentRepository.save(d);
    }

    private AcademicYear createYear(int num, String label) {
        AcademicYear y = new AcademicYear();
        y.setYearNumber(num);
        y.setLabel(label);
        return academicYearRepository.save(y);
    }

    private Section createSection(String name, Department dept, AcademicYear yr) {
        Section s = new Section();
        s.setName(name);
        s.setDepartment(dept);
        s.setAcademicYear(yr);
        s.setDisplayName(dept.getCode() + "-" + name);
        return sectionRepository.save(s);
    }

    private Period createPeriod(int num, LocalTime start, LocalTime end, PeriodType type) {
        Period p = new Period();
        p.setPeriodNumber(num);
        p.setStartTime(start);
        p.setEndTime(end);
        p.setType(type);
        return periodRepository.save(p);
    }

    private Classroom createRoom(String name, RoomType type, int cap) {
        Classroom r = new Classroom();
        r.setName(name);
        r.setRoomType(type);
        r.setCapacity(cap);
        return classroomRepository.save(r);
    }

    private Teacher createTeacher(String teacherId, String name, String email, String phone, Set<Department> depts) {
        User u = new User();
        u.setUsername(teacherId);
        u.setPassword(passwordEncoder.encode("teacher123"));
        u.setRole(UserRole.ROLE_TEACHER);
        u.setFullName(name);
        u.setEmail(email);
        u.setEnabled(true);
        u = userRepository.save(u);

        Teacher t = new Teacher();
        t.setTeacherId(teacherId);
        t.setUser(u);
        t.setPhone(phone);
        t.setDepartments(depts);
        return teacherRepository.save(t);
    }

    private void createStudents(int start, int end, String idFormat, Department dept, AcademicYear yr, Section sec, String namePrefix) {
        for (int i = start; i <= end; i++) {
            String studentId = String.format(idFormat, i);
            User u = new User();
            u.setUsername(studentId);
            u.setPassword(passwordEncoder.encode("student123"));
            u.setRole(UserRole.ROLE_STUDENT);
            u.setFullName(namePrefix + " " + i);
            u.setEmail(studentId.toLowerCase() + "@smartschedule.edu");
            u.setEnabled(true);
            u = userRepository.save(u);

            Student s = new Student();
            s.setStudentId(studentId);
            s.setUser(u);
            s.setDepartment(dept);
            s.setAcademicYear(yr);
            s.setSection(sec);
            s.setSemester(3);
            studentRepository.save(s);
        }
    }

    private Subject createSubject(String name, String code, SubjectType type, int periods, Department dept, AcademicYear yr) {
        Subject s = new Subject();
        s.setName(name);
        s.setCode(code);
        s.setType(type);
        s.setPeriodsPerWeek(periods);
        s.setDepartment(dept);
        s.setAcademicYear(yr);
        return subjectRepository.save(s);
    }

    private void createAssignment(Subject s, Teacher t, Section sec) {
        SubjectAssignment sa = new SubjectAssignment();
        sa.setSubject(s);
        sa.setTeacher(t);
        sa.setSection(sec);
        subjectAssignmentRepository.save(sa);
    }

    private void createEntry(Section sec, DayOfWeek day, Period p, Subject s, Teacher t, Classroom r) {
        TimetableEntry e = new TimetableEntry();
        e.setSection(sec);
        e.setDayOfWeek(day);
        e.setPeriod(p);
        e.setSubject(s);
        e.setTeacher(t);
        e.setClassroom(r);
        e.setStatus(EntryStatus.ACTIVE);
        e.setPublished(true);
        e.setLocked(false);
        timetableEntryRepository.save(e);
    }
}
