# Smart Schedule Assist 📅

A comprehensive, production-grade, responsive full-stack timetable management and scheduling web application built with **Java 21**, **Spring Boot 3.2**, **Spring Security 6**, **Spring Data JPA / Hibernate**, **MySQL**, and **Thymeleaf**.

---

## 🌟 Key Features

### 1. 🎓 Student Experience
- **Personalized Dashboard**: View student profile, current semester, section, and enrolled department.
- **Real-Time Next Class Countdown**: Dynamic timer updating every second showing the next scheduled class room, teacher, and starting time.
- **Weekly & Daily Timetable Grid**: Interactive schedule with day filtering tabs and live status indicators (Scheduled, Free, Cancelled, Substitute).
- **5-Minute Bell Reminders**: Web Notifications API integration alerting students 5 minutes before each class.
- **Official PDF Timetable Export**: Download clean A4 landscape PDF versions of weekly or daily schedules.
- **Targeted Announcements & SSE Notifications**: Real-time push updates for room changes, cancellations, or campus notices.

### 2. 👨‍🏫 Teacher / Faculty Experience
- **Faculty Dashboard & Schedule**: View daily teaching sessions, assigned sections, and classroom allocations.
- **Absence & Leave Management**: Submit full-day or specific-period absence requests with reason and suggested substitutes.
- **Timetable Slot Change Requests**: Request to modify, reschedule, or cancel individual class slots.
- **Substitute Teaching Alerts**: Instant notifications when assigned as a substitute teacher for absent colleagues.
- **Faculty Timetable PDF Export**: Download personal weekly teaching schedule.

### 3. ⚙️ Admin Experience
- **Constraint-Based Timetable Generator**: Intelligent backtracking generator that places theory and lab classes while respecting room types, faculty workloads, teacher availability, and locked slots.
- **Interactive Timetable Grid Editor**: Drag/edit individual slots, lock fixed entries, and publish drafts to students and faculty.
- **Faculty Absence & Substitute Resolution Hub**: Review absence requests, view available free teachers for each affected class slot, assign substitutes, or cancel classes.
- **Change Request Approvals**: Approve or reject faculty slot modifications and cancellations.
- **Bulk Batch Imports**: Import Students, Faculty, Subjects, and Classrooms from CSV or Excel (`.xlsx`, `.xls`) with row-level validation.
- **Department & Academic Management**: Full CRUD for Departments, Academic Years, Sections, Courses, and Classrooms.
- **Audit & Activity Stream**: Real-time activity logging with pagination tracking every administrative and academic event.

---

## 🏗️ Technology Stack

| Layer | Technology |
|---|---|
| **Language** | Java 21 (LTS) |
| **Framework** | Spring Boot 3.2.5 |
| **Security** | Spring Security 6 (BCrypt Password Hashing, Role-Based Access Control) |
| **Persistence** | Spring Data JPA / Hibernate ORM |
| **Database** | MySQL 8.0+ |
| **Template Engine** | Thymeleaf 3 with Spring Security Dialect |
| **PDF Generation** | OpenPDF 1.3.39 (A4 Landscape Tables) |
| **File Processing** | Apache POI 5.2.5 (Excel), OpenCSV 5.9 (CSV) |
| **Real-Time Push** | Server-Sent Events (SSE) via `SseEmitter` |
| **Frontend Styling** | Custom CSS (Responsive CSS Grid & Flexbox, Google Blue Neutral Palette `#1a73e8`) |
| **Frontend Scripts** | Pure Vanilla JavaScript ES6+ (Web Notifications, SSE Stream, Dynamic Modals) |

---

## 🔑 Demo User Accounts

The system automatically initializes seed data on startup (`DataInitializer.java`):

| Role | Username / ID | Password | Notes |
|---|---|---|---|
| **Admin** | `admin` | `admin123` | Full system access |
| **Teacher** | `T001` | `teacher123` | Dr. Alan Turing (CSE) |
| **Teacher** | `T002` | `teacher123` | Prof. Grace Hopper (CSE) |
| **Student** | `S001` | `student123` | John Doe (CSE-A, 2nd Year) |
| **Student** | `S002` | `student123` | Jane Smith (CSE-A, 2nd Year) |

---

## ⚡ Timetable Generation Algorithm

The timetable generator (`TimetableGeneratorService.java`) uses a constraint-satisfaction heuristic with backtracking:

1. **Hard Constraints (Never Violated)**:
   - **Faculty Non-Overlapping**: A teacher cannot teach more than one class at the same day & period across all sections.
   - **Classroom Non-Overlapping**: A room/lab cannot host multiple classes simultaneously.
   - **Room Capability**: Labs require `COMPUTER_LAB`, `PHYSICS_LAB`, or `ELECTRONICS_LAB`; Theory requires standard `CLASSROOM`.
   - **Consecutive Lab Slots**: Lab practicals (e.g., 2 periods/week) are automatically scheduled back-to-back in consecutive class periods.
   - **Locked Slot Preservation**: Any slot manually locked by the administrator (🔒) is preserved and never overwritten during regeneration.
   - **Teacher Availability Matrix**: Checks `TeacherAvailability` records to prevent scheduling during unavailable faculty hours.

2. **Soft Constraints (Optimized by Heuristic Scoring)**:
   - **Subject Distribution**: Prevents multiple theory lectures of the same subject on the same day whenever possible (+10 score bonus).
   - **Weekly Spread**: Evenly balances subject distribution across Monday through Friday.
   - **Morning Theory Preference**: Schedules core theory subjects in morning periods (+2 score bonus).

---

## 📥 Batch Import Specifications

The application supports importing data in CSV and Microsoft Excel (`.xlsx`, `.xls`) formats:

### 1. Students Template
```csv
Student ID,Full Name,Email,Password,Department Code,Year Number,Section Name,Semester
S101,John Doe,john@smartschedule.edu,student123,CSE,2,A,3
S102,Jane Smith,jane@smartschedule.edu,student123,CSE,2,B,3
```

### 2. Teachers Template
```csv
Teacher ID,Full Name,Email,Password,Phone,Department Codes
T101,Dr. Alan Turing,alan@smartschedule.edu,teacher123,9876543210,"CSE,ECE"
T102,Prof. Grace Hopper,grace@smartschedule.edu,teacher123,9876543211,CSE
```

### 3. Subjects Template
```csv
Subject Code,Subject Name,Type,Periods Per Week,Department Code,Year Number
CS301,Artificial Intelligence,THEORY,4,CSE,3
CS302,AI Lab,LAB,2,CSE,3
```

### 4. Classrooms Template
```csv
Room Name,Room Type,Capacity
Room 301,CLASSROOM,60
AI Lab,COMPUTER_LAB,35
```

---

## 🚀 Getting Started & Setup

### Prerequisites
- **Java 21 JDK** or newer installed.
- **MySQL Server 8.0+** running locally.
- **Apache Maven 3.9+** installed.

### 1. Database Configuration
Create the MySQL database:
```sql
CREATE DATABASE smart_schedule CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Update `src/main/resources/application.properties` if your MySQL username/password differ from the defaults:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/smart_schedule?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=root
```

### 2. Build and Run
Build the project with Maven:
```bash
mvn clean package
```

Run the application:
```bash
mvn spring-boot:run
```

Or run the generated JAR:
```bash
java -jar target/smart-schedule-assist-1.0.0.jar
```

### 3. Access Application
Open your web browser and navigate to:
```text
http://localhost:8080
```
Sign in with any of the demo accounts listed above.

---

## 🧪 Testing

Run the test suite:
```bash
mvn test
```

Includes unit tests for:
- `TimetableGeneratorServiceTest`: Validates constraint satisfaction, period placement, and collision avoidance.
- `AbsenceServiceTest`: Validates affected class auto-discovery and approval lifecycle.
- `SubstituteServiceTest`: Validates substitute teacher conflict checking and student notification dispatching.

---

## 📂 Project Architecture

```text
com.smartschedule
├── config                  # SecurityConfig, WebConfig, DataInitializer
├── controller              # AuthController, ApiController, StudentController, TeacherController, AdminController, etc.
│   └── admin               # TimetableController, AbsenceManagementController, StudentManagementController, etc.
├── dto                     # StudentDTO, TeacherDTO, TimetableEntryDTO, ImportResult, GenerationResult, etc.
├── exception               # GlobalExceptionHandler, ResourceNotFoundException, etc.
├── model                   # 22 JPA Entities (User, Student, Teacher, TimetableEntry, AbsenceRequest, etc.)
│   └── enums               # 11 Enums (UserRole, SubjectType, RoomType, EntryStatus, RequestStatus, etc.)
├── repository              # 22 Spring Data JPA Repositories
└── service                 # TimetableGeneratorService, TimetableService, AbsenceService, SubstituteService, PdfService, ImportService, NotificationService, etc.
```
