# Smart Schedule Assist - Complete Specification Reference

## Package Structure
```
com.smartschedule
├── SmartScheduleApplication.java
├── config/          SecurityConfig, WebConfig, DataInitializer
├── security/        CustomUserDetailsService, LoginSuccessHandler
├── model/           JPA entity classes
│   └── enums/       Enum types
├── repository/      Spring Data JPA repos
├── dto/             Data transfer objects
├── service/         Business logic services
├── controller/      Spring MVC controllers
│   └── admin/       Admin-specific controllers
└── exception/       GlobalExceptionHandler
```

Source root: `b:\JavaProject\smart-schedule-assist\src\main\java\com\smartschedule\`
Resources: `b:\JavaProject\smart-schedule-assist\src\main\resources\`
Templates: `b:\JavaProject\smart-schedule-assist\src\main\resources\templates\`
Static: `b:\JavaProject\smart-schedule-assist\src\main\resources\static\`

---

## ENUMS (package: com.smartschedule.model.enums)

```java
// UserRole.java
public enum UserRole { ROLE_STUDENT, ROLE_TEACHER, ROLE_ADMIN }

// SubjectType.java
public enum SubjectType { THEORY, LAB }

// RoomType.java
public enum RoomType { CLASSROOM, COMPUTER_LAB, PHYSICS_LAB, ELECTRONICS_LAB, WORKSHOP, SEMINAR_HALL, OTHER }

// PeriodType.java
public enum PeriodType { CLASS, BREAK, LUNCH }

// EntryStatus.java
public enum EntryStatus { ACTIVE, CANCELLED, SUBSTITUTE }

// RequestStatus.java
public enum RequestStatus { PENDING, APPROVED, REJECTED, WITHDRAWN }

// AbsenceType.java
public enum AbsenceType { FULL_DAY, SPECIFIC_PERIODS }

// NotificationType.java
public enum NotificationType { CLASS_CANCELLATION, TIMETABLE_CHANGE, ABSENCE_APPROVED, ABSENCE_REJECTED, SUBSTITUTE_ASSIGNED, ANNOUNCEMENT, CLASS_REMINDER, SYSTEM }

// TargetType.java
public enum TargetType { EVERYONE, STUDENTS, TEACHERS, DEPARTMENT, YEAR, SECTION }

// ChangeType.java
public enum ChangeType { MODIFY, CANCEL }

// ClassResolution.java
public enum ClassResolution { PENDING, SUBSTITUTE_ASSIGNED, CANCELLED }
```

---

## ENTITIES (package: com.smartschedule.model)

Use `@Getter @Setter @NoArgsConstructor` (NOT @Data) on all entities. Use @AllArgsConstructor only where convenient.

### User
@Entity @Table(name = "users")
| Field | Type | Annotations |
|-------|------|-------------|
| id | Long | @Id @GeneratedValue(IDENTITY) |
| username | String | @Column(unique=true, nullable=false, length=50) |
| password | String | @Column(nullable=false) |
| role | UserRole | @Enumerated(STRING) @Column(nullable=false) |
| email | String | @Column(length=100) |
| fullName | String | @Column(nullable=false, length=100) |
| enabled | boolean | default true |
| createdAt | LocalDateTime | |
| updatedAt | LocalDateTime | |

@PrePersist → createdAt = LocalDateTime.now()
@PreUpdate → updatedAt = LocalDateTime.now()

### Department
@Entity @Table(name = "departments")
| Field | Type | Annotations |
|-------|------|-------------|
| id | Long | @Id @GeneratedValue(IDENTITY) |
| name | String | @Column(nullable=false, length=100) |
| code | String | @Column(unique=true, nullable=false, length=10) |
| createdAt | LocalDateTime | |

### AcademicYear
@Entity @Table(name = "academic_years")
| Field | Type | Annotations |
|-------|------|-------------|
| id | Long | @Id @GeneratedValue(IDENTITY) |
| yearNumber | int | @Column(nullable=false) |
| label | String | @Column(nullable=false) (e.g. "1st Year") |

### Section
@Entity @Table(name = "sections")  
@Table(uniqueConstraints = @UniqueConstraint(columns = {"department_id", "academic_year_id", "name"}))
| Field | Type | Annotations |
|-------|------|-------------|
| id | Long | @Id @GeneratedValue(IDENTITY) |
| name | String | @Column(nullable=false, length=5) (e.g. "A") |
| department | Department | @ManyToOne @JoinColumn(name="department_id", nullable=false) |
| academicYear | AcademicYear | @ManyToOne @JoinColumn(name="academic_year_id", nullable=false) |
| displayName | String | @Column(nullable=false, length=20) (e.g. "CSE-A") |

### Student
@Entity @Table(name = "students")
| Field | Type | Annotations |
|-------|------|-------------|
| id | Long | @Id @GeneratedValue(IDENTITY) |
| studentId | String | @Column(unique=true, nullable=false, length=20) |
| user | User | @OneToOne(cascade=ALL, fetch=EAGER) @JoinColumn(name="user_id") |
| department | Department | @ManyToOne @JoinColumn(name="department_id") |
| academicYear | AcademicYear | @ManyToOne @JoinColumn(name="academic_year_id") |
| section | Section | @ManyToOne @JoinColumn(name="section_id") |
| semester | int | |

### Teacher
@Entity @Table(name = "teachers")
| Field | Type | Annotations |
|-------|------|-------------|
| id | Long | @Id @GeneratedValue(IDENTITY) |
| teacherId | String | @Column(unique=true, nullable=false, length=20) |
| user | User | @OneToOne(cascade=ALL, fetch=EAGER) @JoinColumn(name="user_id") |
| departments | Set\<Department\> | @ManyToMany @JoinTable(name="teacher_departments") |
| phone | String | @Column(length=20) |

### Subject
@Entity @Table(name = "subjects")
| Field | Type | Annotations |
|-------|------|-------------|
| id | Long | @Id @GeneratedValue(IDENTITY) |
| name | String | @Column(nullable=false, length=100) |
| code | String | @Column(unique=true, nullable=false, length=20) |
| type | SubjectType | @Enumerated(STRING) |
| periodsPerWeek | int | |
| department | Department | @ManyToOne @JoinColumn(name="department_id") |
| academicYear | AcademicYear | @ManyToOne @JoinColumn(name="academic_year_id") |

### SubjectAssignment
@Entity @Table(name = "subject_assignments")
@Table(uniqueConstraints = @UniqueConstraint(columns = {"subject_id", "section_id"}))
| Field | Type | Annotations |
|-------|------|-------------|
| id | Long | @Id @GeneratedValue(IDENTITY) |
| subject | Subject | @ManyToOne @JoinColumn(name="subject_id", nullable=false) |
| teacher | Teacher | @ManyToOne @JoinColumn(name="teacher_id", nullable=false) |
| section | Section | @ManyToOne @JoinColumn(name="section_id", nullable=false) |

### Classroom
@Entity @Table(name = "classrooms")
| Field | Type | Annotations |
|-------|------|-------------|
| id | Long | @Id @GeneratedValue(IDENTITY) |
| name | String | @Column(nullable=false, unique=true, length=50) |
| roomType | RoomType | @Enumerated(STRING) |
| capacity | int | |

### Period
@Entity @Table(name = "periods")
| Field | Type | Annotations |
|-------|------|-------------|
| id | Long | @Id @GeneratedValue(IDENTITY) |
| periodNumber | int | @Column(nullable=false, unique=true) |
| startTime | LocalTime | @Column(nullable=false) |
| endTime | LocalTime | @Column(nullable=false) |
| type | PeriodType | @Enumerated(STRING) @Column(nullable=false) |

### TimetableEntry
@Entity @Table(name = "timetable_entries")
| Field | Type | Annotations |
|-------|------|-------------|
| id | Long | @Id @GeneratedValue(IDENTITY) |
| section | Section | @ManyToOne @JoinColumn(name="section_id", nullable=false) |
| dayOfWeek | java.time.DayOfWeek | @Enumerated(STRING) @Column(nullable=false) |
| period | Period | @ManyToOne @JoinColumn(name="period_id", nullable=false) |
| subject | Subject | @ManyToOne @JoinColumn(name="subject_id") |
| teacher | Teacher | @ManyToOne @JoinColumn(name="teacher_id") |
| classroom | Classroom | @ManyToOne @JoinColumn(name="classroom_id") |
| status | EntryStatus | @Enumerated(STRING) default ACTIVE |
| substituteTeacher | Teacher | @ManyToOne @JoinColumn(name="substitute_teacher_id") nullable |
| locked | boolean | default false |
| published | boolean | default false |
| version | int | default 1 |

### TeacherAvailability
@Entity @Table(name = "teacher_availability")
| Field | Type | Annotations |
|-------|------|-------------|
| id | Long | @Id @GeneratedValue(IDENTITY) |
| teacher | Teacher | @ManyToOne @JoinColumn(name="teacher_id", nullable=false) |
| dayOfWeek | java.time.DayOfWeek | @Enumerated(STRING) |
| period | Period | @ManyToOne @JoinColumn(name="period_id", nullable=false) |
| available | boolean | default true |

### AbsenceRequest
@Entity @Table(name = "absence_requests")
| Field | Type | Annotations |
|-------|------|-------------|
| id | Long | @Id @GeneratedValue(IDENTITY) |
| teacher | Teacher | @ManyToOne @JoinColumn(name="teacher_id", nullable=false) |
| absenceDate | LocalDate | @Column(nullable=false) |
| absenceType | AbsenceType | @Enumerated(STRING) |
| reason | String | @Column(columnDefinition="TEXT") nullable |
| status | RequestStatus | @Enumerated(STRING) default PENDING |
| adminComment | String | nullable |
| suggestedSubstitute | Teacher | @ManyToOne @JoinColumn nullable |
| affectedClasses | List\<AbsenceAffectedClass\> | @OneToMany(mappedBy="absenceRequest", cascade=ALL, orphanRemoval=true) |
| createdAt | LocalDateTime | |
| reviewedAt | LocalDateTime | nullable |
| reviewedBy | User | @ManyToOne nullable |

### AbsenceAffectedClass
@Entity @Table(name = "absence_affected_classes")
| Field | Type | Annotations |
|-------|------|-------------|
| id | Long | @Id @GeneratedValue(IDENTITY) |
| absenceRequest | AbsenceRequest | @ManyToOne @JoinColumn(nullable=false) |
| timetableEntry | TimetableEntry | @ManyToOne @JoinColumn(nullable=false) |
| period | Period | @ManyToOne @JoinColumn(nullable=false) |
| resolution | ClassResolution | @Enumerated(STRING) default PENDING |

### SubstituteAssignment
@Entity @Table(name = "substitute_assignments")
| Field | Type | Annotations |
|-------|------|-------------|
| id | Long | @Id @GeneratedValue(IDENTITY) |
| affectedClass | AbsenceAffectedClass | @OneToOne @JoinColumn(nullable=false) |
| substituteTeacher | Teacher | @ManyToOne @JoinColumn(nullable=false) |
| assignedBy | User | @ManyToOne @JoinColumn(nullable=false) |
| createdAt | LocalDateTime | |

### TimetableChangeRequest
@Entity @Table(name = "timetable_change_requests")
| Field | Type | Annotations |
|-------|------|-------------|
| id | Long | @Id @GeneratedValue(IDENTITY) |
| teacher | Teacher | @ManyToOne @JoinColumn(nullable=false) |
| timetableEntry | TimetableEntry | @ManyToOne @JoinColumn(nullable=false) |
| changeType | ChangeType | @Enumerated(STRING) |
| requestedChanges | String | @Column(columnDefinition="TEXT") |
| reason | String | |
| status | RequestStatus | @Enumerated(STRING) default PENDING |
| adminComment | String | nullable |
| createdAt | LocalDateTime | |
| reviewedAt | LocalDateTime | nullable |

### Notification
@Entity @Table(name = "notifications")
| Field | Type | Annotations |
|-------|------|-------------|
| id | Long | @Id @GeneratedValue(IDENTITY) |
| user | User | @ManyToOne @JoinColumn(nullable=false) |
| title | String | @Column(nullable=false) |
| message | String | @Column(columnDefinition="TEXT") |
| type | NotificationType | @Enumerated(STRING) |
| isRead | boolean | @Column(name="is_read") default false |
| relatedEntityType | String | nullable |
| relatedEntityId | Long | nullable |
| createdAt | LocalDateTime | |

### Announcement
@Entity @Table(name = "announcements")
| Field | Type | Annotations |
|-------|------|-------------|
| id | Long | @Id @GeneratedValue(IDENTITY) |
| title | String | @Column(nullable=false) |
| content | String | @Column(columnDefinition="TEXT") |
| createdBy | User | @ManyToOne @JoinColumn(nullable=false) |
| targetType | TargetType | @Enumerated(STRING) |
| targets | List\<AnnouncementTarget\> | @OneToMany(mappedBy="announcement", cascade=ALL, orphanRemoval=true) |
| scheduledAt | LocalDateTime | nullable |
| publishedAt | LocalDateTime | nullable |
| active | boolean | default true |
| createdAt | LocalDateTime | |

### AnnouncementTarget
@Entity @Table(name = "announcement_targets")
| Field | Type | Annotations |
|-------|------|-------------|
| id | Long | @Id @GeneratedValue(IDENTITY) |
| announcement | Announcement | @ManyToOne @JoinColumn(nullable=false) |
| targetType | TargetType | @Enumerated(STRING) |
| targetId | Long | nullable |

### ActivityLog
@Entity @Table(name = "activity_logs")
| Field | Type | Annotations |
|-------|------|-------------|
| id | Long | @Id @GeneratedValue(IDENTITY) |
| action | String | @Column(nullable=false) |
| entityType | String | |
| entityId | Long | nullable |
| performedBy | User | @ManyToOne @JoinColumn |
| details | String | @Column(columnDefinition="TEXT") |
| ipAddress | String | nullable |
| timestamp | LocalDateTime | |

### SystemSettings
@Entity @Table(name = "system_settings")
| Field | Type | Annotations |
|-------|------|-------------|
| id | Long | @Id @GeneratedValue(IDENTITY) |
| settingKey | String | @Column(unique=true, nullable=false) |
| settingValue | String | @Column(columnDefinition="TEXT") |

### PasswordResetToken
@Entity @Table(name = "password_reset_tokens")
| Field | Type | Annotations |
|-------|------|-------------|
| id | Long | @Id @GeneratedValue(IDENTITY) |
| token | String | @Column(unique=true, nullable=false) |
| user | User | @OneToOne @JoinColumn(nullable=false) |
| expiryDate | LocalDateTime | |

---

## KEY REPOSITORY METHODS

All repos extend `JpaRepository<Entity, Long>`.

### UserRepository
- `Optional<User> findByUsername(String username)`
- `boolean existsByUsername(String username)`

### StudentRepository
- `Optional<Student> findByStudentId(String studentId)`
- `Optional<Student> findByUser(User user)`
- `List<Student> findBySection(Section section)`
- `List<Student> findByDepartment(Department department)`
- `List<Student> findByDepartmentAndAcademicYearAndSection(Department d, AcademicYear y, Section s)`
- `boolean existsByStudentId(String studentId)`

### TeacherRepository
- `Optional<Teacher> findByTeacherId(String teacherId)`
- `Optional<Teacher> findByUser(User user)`
- `List<Teacher> findByDepartmentsContaining(Department department)`
- `boolean existsByTeacherId(String teacherId)`

### SectionRepository
- `List<Section> findByDepartment(Department department)`
- `List<Section> findByDepartmentAndAcademicYear(Department d, AcademicYear y)`
- `Optional<Section> findByDepartmentAndAcademicYearAndName(Department d, AcademicYear y, String name)`

### SubjectRepository
- `List<Subject> findByDepartmentAndAcademicYear(Department d, AcademicYear y)`
- `Optional<Subject> findByCode(String code)`
- `boolean existsByCode(String code)`

### SubjectAssignmentRepository
- `List<SubjectAssignment> findBySection(Section section)`
- `List<SubjectAssignment> findByTeacher(Teacher teacher)`
- `Optional<SubjectAssignment> findBySubjectAndSection(Subject s, Section sec)`

### TimetableEntryRepository
- `List<TimetableEntry> findBySectionAndPublished(Section s, boolean pub)`
- `List<TimetableEntry> findBySectionAndDayOfWeekAndPublished(Section s, DayOfWeek d, boolean pub)`
- `List<TimetableEntry> findByTeacherAndPublished(Teacher t, boolean pub)`
- `List<TimetableEntry> findByTeacherAndDayOfWeekAndPublished(Teacher t, DayOfWeek d, boolean pub)`
- `List<TimetableEntry> findByPublished(boolean pub)`

### PeriodRepository
- `List<Period> findByTypeOrderByPeriodNumberAsc(PeriodType type)`
- `List<Period> findAllByOrderByPeriodNumberAsc()`
- `Optional<Period> findByPeriodNumber(int n)`

### NotificationRepository
- `List<Notification> findByUserOrderByCreatedAtDesc(User user)`
- `List<Notification> findByUserAndIsReadOrderByCreatedAtDesc(User user, boolean read)`
- `long countByUserAndIsRead(User user, boolean read)`

### AbsenceRequestRepository
- `List<AbsenceRequest> findByTeacher(Teacher teacher)`
- `List<AbsenceRequest> findByStatus(RequestStatus status)`
- `List<AbsenceRequest> findByStatusOrderByCreatedAtDesc(RequestStatus status)`

### AnnouncementRepository
- `List<Announcement> findByActiveOrderByCreatedAtDesc(boolean active)`

### ActivityLogRepository
- `Page<ActivityLog> findAllByOrderByTimestampDesc(Pageable pageable)`

### Other repos
Standard JpaRepository methods are sufficient for: DepartmentRepository, AcademicYearRepository, ClassroomRepository, TeacherAvailabilityRepository, AbsenceAffectedClassRepository, SubstituteAssignmentRepository, TimetableChangeRequestRepository, AnnouncementTargetRepository, SystemSettingsRepository, PasswordResetTokenRepository. Add `findByXxx` methods as needed.

---

## PERIOD SCHEDULE (seeded by DataInitializer)

| # | Type | Start | End |
|---|------|-------|-----|
| 1 | CLASS | 09:00 | 09:50 |
| 2 | BREAK | 09:50 | 10:00 |
| 3 | CLASS | 10:00 | 10:50 |
| 4 | BREAK | 10:50 | 11:10 |
| 5 | CLASS | 11:10 | 12:00 |
| 6 | CLASS | 12:00 | 12:50 |
| 7 | LUNCH | 12:50 | 13:40 |
| 8 | CLASS | 13:40 | 14:30 |
| 9 | BREAK | 14:30 | 14:40 |
| 10 | CLASS | 14:40 | 15:30 |

Academic periods are 1, 3, 5, 6, 8, 10 (six CLASS periods).

---

## SAMPLE DATA (seeded by DataInitializer)

### Admin
- Username: `admin`, Password: `admin123` (BCrypt), Role: ROLE_ADMIN, Name: "System Administrator"

### Departments
CSE, ECE, EEE, Civil, Mechanical

### Academic Years
1st Year (1), 2nd Year (2), 3rd Year (3), 4th Year (4)

### Sections (2nd Year primarily)
CSE-A, CSE-B, ECE-A, ECE-B, EEE-A

### Teachers (15)
| ID | Name | Departments |
|----|------|-------------|
| T001 | Dr. Kumar | CSE |
| T002 | Prof. Sharma | CSE |
| T003 | Dr. Patel | CSE, ECE |
| T004 | Prof. Reddy | ECE |
| T005 | Dr. Singh | ECE |
| T006 | Prof. Gupta | EEE |
| T007 | Dr. Verma | EEE |
| T008 | Prof. Iyer | CSE |
| T009 | Dr. Nair | Civil |
| T010 | Prof. Das | Civil |
| T011 | Dr. Joshi | Mechanical |
| T012 | Prof. Mehta | Mechanical |
| T013 | Dr. Rao | CSE, ECE |
| T014 | Prof. Pillai | EEE, ECE |
| T015 | Dr. Bhat | Civil, Mechanical |

All teacher passwords: `teacher123` (BCrypt)

### Students (30)
S001–S006: CSE-A 2nd Year, Semester 3
S007–S012: CSE-B 2nd Year, Semester 3
S013–S018: ECE-A 2nd Year, Semester 3
S019–S024: ECE-B 2nd Year, Semester 3
S025–S030: EEE-A 2nd Year, Semester 3
All passwords: `student123` (BCrypt)

### Subjects (CSE 2nd Year)
| Code | Name | Type | Periods/Week |
|------|------|------|-------------|
| CS201 | Data Structures | THEORY | 4 |
| CS202 | Database Management Systems | THEORY | 4 |
| CS203 | Object Oriented Programming | THEORY | 3 |
| CS204 | Computer Networks | THEORY | 3 |
| CS205 | Operating Systems | THEORY | 4 |
| CS206 | Data Structures Lab | LAB | 2 |
| CS207 | DBMS Lab | LAB | 2 |
| CS208 | OOP Lab | LAB | 2 |

### Subjects (ECE 2nd Year)
| Code | Name | Type | Periods/Week |
|------|------|------|-------------|
| EC201 | Signals and Systems | THEORY | 4 |
| EC202 | Digital Electronics | THEORY | 4 |
| EC203 | Electromagnetic Theory | THEORY | 3 |
| EC204 | Network Analysis | THEORY | 3 |
| EC205 | Electronic Circuits | THEORY | 4 |
| EC206 | Electronics Lab | LAB | 2 |
| EC207 | Digital Lab | LAB | 2 |

### Subjects (EEE 2nd Year)
| Code | Name | Type | Periods/Week |
|------|------|------|-------------|
| EE201 | Circuit Theory | THEORY | 4 |
| EE202 | Electrical Machines | THEORY | 4 |
| EE203 | Power Systems | THEORY | 3 |
| EE204 | Basic Electronics | THEORY | 3 |
| EE205 | Control Systems | THEORY | 4 |
| EE206 | Electrical Lab | LAB | 2 |

### Classrooms
| Name | Type | Capacity |
|------|------|----------|
| Room 101 | CLASSROOM | 60 |
| Room 102 | CLASSROOM | 60 |
| Room 103 | CLASSROOM | 60 |
| Room 104 | CLASSROOM | 60 |
| Room 105 | CLASSROOM | 40 |
| Room 201 | CLASSROOM | 60 |
| Room 202 | CLASSROOM | 60 |
| CS Lab 1 | COMPUTER_LAB | 30 |
| CS Lab 2 | COMPUTER_LAB | 30 |
| Electronics Lab | ELECTRONICS_LAB | 30 |
| Physics Lab | PHYSICS_LAB | 30 |
| Seminar Hall | SEMINAR_HALL | 200 |

### System Settings
- `current_semester`: "3"
- `academic_year`: "2026-2027"

---

## URL STRUCTURE

### Public
- `GET /login` — login page
- `POST /login` — Spring Security handles
- `GET /forgot-password` — forgot password form
- `POST /forgot-password` — submit email for reset
- `GET /reset-password?token=xxx` — reset form
- `POST /reset-password` — save new password

### Student (/student/**)
- `GET /student/dashboard` — main dashboard
- `GET /student/timetable` — weekly timetable view
- `GET /student/timetable/day/{dayName}` — specific day
- `GET /student/notifications` — notification center
- `GET /student/announcements` — announcements
- `GET /student/profile` — view profile
- `GET /student/change-password` — change password form
- `POST /student/change-password` — submit password change
- `GET /student/timetable/pdf` — download weekly PDF
- `GET /student/timetable/pdf/today` — download today PDF

### Teacher (/teacher/**)
- `GET /teacher/dashboard` — main dashboard
- `GET /teacher/timetable` — weekly timetable
- `GET /teacher/timetable/day/{dayName}` — specific day
- `GET /teacher/absence` — absence request list + form
- `POST /teacher/absence` — submit absence request
- `POST /teacher/absence/{id}/withdraw` — withdraw request
- `GET /teacher/change-requests` — change request list
- `POST /teacher/change-request` — submit change request
- `POST /teacher/cancel-request` — submit cancellation request
- `GET /teacher/notifications` — notification center
- `GET /teacher/profile` — view profile
- `GET /teacher/change-password` — change password form
- `POST /teacher/change-password` — submit password change
- `GET /teacher/timetable/pdf` — download weekly PDF

### Admin (/admin/**)
- `GET /admin/dashboard` — main dashboard with stats
- **Departments**: `GET/POST /admin/departments`, `GET/POST /admin/departments/edit/{id}`, `POST /admin/departments/delete/{id}`
- **Sections**: `GET/POST /admin/sections`, `GET/POST /admin/sections/edit/{id}`, `POST /admin/sections/delete/{id}`
- **Subjects**: `GET/POST /admin/subjects`, `GET/POST /admin/subjects/edit/{id}`, `POST /admin/subjects/delete/{id}`
- **Classrooms**: `GET/POST /admin/classrooms`, `GET/POST /admin/classrooms/edit/{id}`, `POST /admin/classrooms/delete/{id}`
- **Students**: `GET /admin/students`, `GET/POST /admin/students/create`, `GET/POST /admin/students/edit/{id}`, `POST /admin/students/delete/{id}`, `POST /admin/students/import`
- **Teachers**: `GET /admin/teachers`, `GET/POST /admin/teachers/create`, `GET/POST /admin/teachers/edit/{id}`, `POST /admin/teachers/delete/{id}`, `POST /admin/teachers/import`
- **Timetables**: `GET /admin/timetables` (filter by dept/year/section), `GET /admin/timetables/edit/{sectionId}`, `POST /admin/timetables/save`, `POST /admin/timetables/generate`, `POST /admin/timetables/publish/{sectionId}`, `GET /admin/timetables/teacher/{teacherId}`
- **Absence Requests**: `GET /admin/absence-requests`, `GET /admin/absence-requests/{id}`, `POST /admin/absence-requests/{id}/approve`, `POST /admin/absence-requests/{id}/reject`
- **Substitutes**: `GET /admin/substitutes/{affectedClassId}`, `POST /admin/substitutes/assign`
- **Timetable Requests**: `GET /admin/timetable-requests`, `POST /admin/timetable-requests/{id}/approve`, `POST /admin/timetable-requests/{id}/reject`
- **Announcements**: `GET/POST /admin/announcements`, `GET/POST /admin/announcements/edit/{id}`, `POST /admin/announcements/delete/{id}`
- **Notifications**: `GET /admin/notifications`
- **Settings**: `GET/POST /admin/settings`
- **Activity Log**: `GET /admin/activity-log`
- **Search**: `GET /admin/search?q=xxx`

### REST API
- `GET /api/notifications/stream` — SSE endpoint for real-time notifications
- `GET /api/notifications/unread-count` — JSON unread count
- `POST /api/notifications/{id}/read` — mark notification read
- `POST /api/notifications/read-all` — mark all read
- `GET /api/timetable/next-class` — JSON next class info for current user
- `GET /api/teachers/available?day=X&period=Y` — available teachers for substitution

---

## UI COLOR PALETTE

| Usage | Hex |
|-------|-----|
| Primary | #1a73e8 |
| Primary Dark | #1557b0 |
| Primary Light | #e8f0fe |
| Background | #f8f9fa |
| Surface/Card | #ffffff |
| Text Primary | #202124 |
| Text Secondary | #5f6368 |
| Border | #dadce0 |
| Success/Teal | #0d904f |
| Warning/Amber | #e37400 |
| Error/Coral | #d93025 |
| Cancelled/Muted | #9aa0a6 |

---

## KEY SERVICE METHODS

### StudentService
- `Student getStudentByUser(User user)`
- `Student getStudentByStudentId(String studentId)`
- `List<Student> getStudentsBySection(Section section)`
- `Student createStudent(StudentDTO dto)` — creates User + Student
- `Student updateStudent(Long id, StudentDTO dto)`
- `void deleteStudent(Long id)`
- `List<Student> getAllStudents()`
- `ImportResult importStudents(MultipartFile file)` — CSV/Excel

### TeacherService
- `Teacher getTeacherByUser(User user)`
- `Teacher getTeacherByTeacherId(String teacherId)`
- `Teacher createTeacher(TeacherDTO dto)` — creates User + Teacher
- `Teacher updateTeacher(Long id, TeacherDTO dto)`
- `void deleteTeacher(Long id)`
- `List<Teacher> getAllTeachers()`
- `List<Teacher> getAvailableTeachers(DayOfWeek day, Period period)` — for substitution
- `ImportResult importTeachers(MultipartFile file)`

### TimetableService
- `List<TimetableEntry> getTimetableBySection(Section section)` — published only
- `List<TimetableEntry> getTimetableBySectionAndDay(Section section, DayOfWeek day)`
- `List<TimetableEntry> getTimetableByTeacher(Teacher teacher)` — published only
- `List<TimetableEntry> getTimetableByTeacherAndDay(Teacher teacher, DayOfWeek day)`
- `TimetableEntry getNextClass(Section section)` — skip FREE/CANCELLED
- `TimetableEntry getNextClassForTeacher(Teacher teacher)`
- `Map<DayOfWeek, List<TimetableEntry>> getWeeklyTimetable(Section section)`
- `Map<DayOfWeek, List<TimetableEntry>> getWeeklyTeacherTimetable(Teacher teacher)`
- `List<TimetableEntry> getDraftTimetable(Section section)` — unpublished
- `void saveTimetableEntry(TimetableEntry entry)`
- `void publishTimetable(Section section)` — mark all draft as published
- `void updateEntryStatus(Long entryId, EntryStatus status)`

### TimetableGeneratorService
- `List<TimetableEntry> generateTimetable(Section section)` — constraint backtracking
- `GenerationResult generateAndValidate(Section section)` — returns result with stats
- `List<TimetableEntry> regenerateTimetable(Section section)` — preserve locked slots

### AbsenceService
- `AbsenceRequest submitAbsence(AbsenceRequestDTO dto)`
- `AbsenceRequest approveAbsence(Long requestId, String adminComment, User admin)`
- `AbsenceRequest rejectAbsence(Long requestId, String adminComment, User admin)`
- `void withdrawAbsence(Long requestId, Teacher teacher)`
- `List<AbsenceRequest> getPendingRequests()`
- `List<AbsenceRequest> getTeacherAbsences(Teacher teacher)`
- `List<AbsenceAffectedClass> getAffectedClasses(AbsenceRequest request)`

### SubstituteService
- `List<Teacher> getAvailableSubstitutes(DayOfWeek day, Period period)`
- `SubstituteAssignment assignSubstitute(Long affectedClassId, Long substituteTeacherId, User admin)`
- `void cancelClass(Long affectedClassId, User admin)`

### NotificationService
- `void createNotification(User user, String title, String message, NotificationType type)`
- `void notifyStudentsInSection(Section section, String title, String message, NotificationType type)`
- `List<Notification> getUserNotifications(User user)`
- `long getUnreadCount(User user)`
- `void markAsRead(Long notificationId)`
- `void markAllAsRead(User user)`
- `SseEmitter subscribe(User user)` — SSE subscription

### AnnouncementService
- `Announcement createAnnouncement(AnnouncementDTO dto, User admin)`
- `List<Announcement> getAnnouncementsForUser(User user)` — filtered by targets
- `List<Announcement> getAllAnnouncements()`

### ActivityLogService
- `void log(String action, String entityType, Long entityId, User user, String details)`
- `Page<ActivityLog> getActivityLogs(Pageable pageable)`

### PdfService
- `byte[] generateSectionTimetablePdf(Section section)`
- `byte[] generateTeacherTimetablePdf(Teacher teacher)`
- `byte[] generateTodayTimetablePdf(Section section)`
- `byte[] generateDepartmentTimetablePdf(Department dept, AcademicYear year)`

### ImportService
- `ImportResult importStudentsFromFile(MultipartFile file)`
- `ImportResult importTeachersFromFile(MultipartFile file)`
- `ImportResult importSubjectsFromFile(MultipartFile file)`
- `ImportResult importClassroomsFromFile(MultipartFile file)`

ImportResult: { int successCount, int errorCount, List<String> errors, List<String> warnings }

---

## THYMELEAF TEMPLATE STRUCTURE

### Shared Fragments (templates/fragments/)
- `layout.html` — main layout with header, sidebar, content area, footer. Uses th:fragment="layout(title, content)". 
  Sidebar varies by role (use sec:authorize).
- `header.html` — top navbar with user info, notification bell (unread badge), logout
- `sidebar.html` — navigation sidebar (different items per role)

### Auth Pages (templates/auth/)
- `login.html` — Student ID/Teacher ID/Admin username + password, role-agnostic
- `forgot-password.html` — email input form
- `reset-password.html` — new password form

### Error Pages (templates/error/)
- `403.html` — Access Denied
- `404.html` — Not Found
- `500.html` — Server Error

### Student Pages (templates/student/)
- `dashboard.html` — header info card, next class card, today's timetable table, weekly timetable summary
- `timetable.html` — day/week toggle, full timetable grid (desktop) / card list (mobile)
- `notifications.html` — notification list with read/unread styling
- `announcements.html` — announcement cards
- `profile.html` — read-only profile info
- `change-password.html` — password change form

### Teacher Pages (templates/teacher/)
- `dashboard.html` — header info card, next class card, today's timetable (shows dept/year/section per entry)
- `timetable.html` — day/week timetable views
- `absence.html` — absence request form (date picker, type, period selector, reason, optional substitute suggestion) + request history list
- `change-requests.html` — list of submitted change requests with status
- `notifications.html`
- `profile.html`
- `change-password.html`

### Admin Pages (templates/admin/)
- `dashboard.html` — stats cards (counts), recent activity, pending items
- `departments.html` — CRUD table with add/edit modal
- `sections.html` — CRUD table filtered by department + year
- `subjects.html` — CRUD table with teacher assignment
- `classrooms.html` — CRUD table
- `students.html` — list with search/filter, create/edit forms, import button
- `student-form.html` — create/edit student form
- `teachers.html` — list with search/filter
- `teacher-form.html` — create/edit teacher form (multi-department select)
- `timetables.html` — section selector, timetable grid, generate/publish buttons
- `timetable-edit.html` — interactive grid editor with drag-and-drop
- `absence-requests.html` — list of pending/all requests
- `absence-detail.html` — request detail with affected classes, substitute assignment UI
- `timetable-requests.html` — list of change/cancellation requests
- `announcements.html` — CRUD for announcements with target selector
- `announcement-form.html` — create/edit announcement
- `notifications.html` — admin notification center
- `settings.html` — period timings, current semester, academic year
- `activity-log.html` — paginated log table
- `search.html` — global search results
- `import.html` — file upload with validation results

All templates use the shared layout fragment.
