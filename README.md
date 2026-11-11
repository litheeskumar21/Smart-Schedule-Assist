# Smart Schedule Assist 📅🤖

A comprehensive, AI-powered, production-grade, responsive full-stack **college timetable management and scheduling web application** built with **Java 21**, **Spring Boot 3.2**, **Spring Security 6**, **Spring Data JPA / Hibernate**, **MySQL 8**, **Thymeleaf 3**, and **local AI via Ollama**.

The system automates timetable generation, teacher absence management, intelligent substitute assignment, policy enforcement, and real-time notifications — all running entirely on a local network with **zero paid API costs**.

---

## 📑 Table of Contents

- [Key Features](#-key-features)
- [AI-Powered Capabilities](#-ai-powered-capabilities)
- [Technology Stack](#️-technology-stack)
- [System Architecture](#-system-architecture)
- [Timetable Generation Algorithm](#-timetable-generation-algorithm)
- [Absence Management & Policy Enforcement](#-absence-management--policy-enforcement)
- [Automated Substitute Assignment](#-automated-substitute-assignment)
- [Real-Time Notification System](#-real-time-notification-system)
- [Batch Import Specifications](#-batch-import-specifications)
- [Demo User Accounts](#-demo-user-accounts)
- [Getting Started & Setup](#-getting-started--setup)
- [Configuration Reference](#️-configuration-reference)
- [Testing](#-testing)
- [Project Architecture](#-project-architecture)
- [API Endpoints](#-api-endpoints)
- [Screenshots & UI](#-screenshots--ui)
- [License](#-license)

---

## 🌟 Key Features

### 1. 🎓 Student Experience
- **Personalized Dashboard** — View student profile, current semester, section, and enrolled department at a glance.
- **Real-Time Next Class Countdown** — Dynamic timer updating every second showing the next scheduled classroom, teacher, and start time.
- **Weekly & Daily Timetable Grid** — Interactive schedule with day-filtering tabs and live status indicators (**Scheduled**, **Free**, **Cancelled**, **Substitute**).
- **5-Minute Bell Reminders** — Web Notifications API integration alerting students 5 minutes before each class begins.
- **Official PDF Timetable Export** — Download clean A4 landscape PDF versions of weekly or daily schedules.
- **Targeted Announcements & Real-Time Notifications** — Server-Sent Events (SSE) push updates for room changes, cancellations, substitute assignments, or campus-wide notices.
- **Profile & Password Management** — View profile details and securely change password.

### 2. 👨‍🏫 Teacher / Faculty Experience
- **Faculty Dashboard & Schedule** — View daily teaching sessions, assigned sections, and classroom allocations with a live next-class countdown.
- **AI-Assisted Absence & Leave Requests** — Submit full-day or specific-period absence requests using natural language; the local AI model parses dates, periods, and reasons automatically.
- **Absence Policy Dashboard** — Real-time visibility into weekly request count, monthly leave-day usage, and remaining allowances before submitting.
- **Timetable Slot Change Requests** — Request to modify, reschedule, or cancel individual class slots with reason and admin approval workflow.
- **Substitute Teaching Alerts** — Instant notifications when assigned as a substitute teacher for an absent colleague.
- **Faculty Timetable PDF Export** — Download personal weekly teaching schedule as PDF.
- **Request Withdrawal** — Withdraw pending absence requests before admin review.
- **Profile & Password Management** — View and update credentials.

### 3. ⚙️ Admin Experience
- **AI-Powered Constraint-Based Timetable Generator** — Intelligent heuristic-scoring generator that places theory and lab classes while respecting room types, faculty workloads, teacher availability, locked slots, and cross-section conflicts. Produces varied, valid schedules on every regeneration.
- **Interactive Timetable Grid Editor** — Click to edit individual slots, lock fixed entries (🔒), and publish drafts to students and faculty with full hard-constraint validation.
- **Faculty Absence & Substitute Resolution Hub** — Review absence requests, view AI-calculated replacement proposals for each affected slot, approve/reject with policy override capability.
- **Automated Replacement Plan Calculation** — One-click calculation of optimal substitutes ranked by qualification, workload balance, and availability across all affected slots.
- **Absence Policy Enforcement & Escalation** — Automatic detection of policy violations (weekly request limits, monthly leave-day caps) with mandatory admin escalation and override tracking.
- **Change Request Approvals** — Approve or reject faculty slot modifications and cancellations with audit trail.
- **Bulk Batch Imports** — Import Students, Faculty, Subjects, and Classrooms from CSV or Excel (`.xlsx`, `.xls`) with row-level validation and error reporting.
- **Department & Academic Management** — Full CRUD for Departments, Academic Years, Sections, Courses, Subjects, and Classrooms.
- **Subject Assignment Management** — Assign teachers to subjects per section with workload tracking.
- **Announcement System** — Create, edit, and publish announcements targeted to specific audiences (Everyone, Students, Teachers, Department, Year, Section).
- **Global Search** — Search across students, teachers, subjects, sections, departments, and classrooms from a single search bar.
- **Configurable System Settings** — Adjust academic year, semester, policy limits, workload caps, and period timings from the UI.
- **Audit & Activity Stream** — Paginated real-time activity logging tracking every administrative and academic event with user, timestamp, and IP address.
- **Department-Wide PDF Export** — Download entire department timetable as a consolidated PDF.
- **Password Reset Administration** — Token-based password reset flow for all users.

### 4. 🎨 Modern UI / UX
- **Role-Based Theme System** — Distinct visual themes for Admin, Teacher, and Student dashboards.
- **Dark Mode Toggle** — System-wide dark mode with persistent preference.
- **Responsive Design** — CSS Grid & Flexbox layout with mobile-optimized views.
- **Google Blue Neutral Palette** — Clean, professional design using `#1a73e8` as the primary accent.
- **Custom Error Pages** — Styled 403, 404, and 500 error pages.

---

## 🤖 AI-Powered Capabilities

Smart Schedule Assist integrates a **local AI language model** via [Ollama](https://ollama.com) for intelligent assistance — with **zero cloud dependency** and **zero API costs**.

| Capability | Description |
|---|---|
| **Natural Language Absence Parsing** | Teachers can type free-text absence requests (e.g., *"I need leave on Monday for a dental appointment"*). The AI extracts date, absence type, affected periods, and reason into structured data. |
| **Replacement Proposal Explanations** | AI generates concise professional explanations for why a specific substitute teacher was selected, citing qualification match and workload balance. |
| **Timetable Generation Summaries** | After auto-generating a timetable, the AI produces a human-readable summary of the result for administrators. |
| **Policy Violation Briefings** | When an absence request violates institutional policy, the AI generates an administrative briefing summarizing the violation context. |
| **Graceful Offline Fallback** | If Ollama is unavailable, every AI feature falls back to deterministic rule-based processing — the application never breaks due to AI downtime. |

**Model**: `qwen2.5:3b` (3.1B parameters, ~1.9 GB, runs on CPU or GPU)
**Hardware Tested**: NVIDIA RTX 4050 6 GB GPU, 16 GB RAM — inference completes in ~2–4 seconds.

---

## 🏗️ Technology Stack

| Layer | Technology |
|---|---|
| **Language** | Java 21 (LTS) |
| **Framework** | Spring Boot 3.2.5 |
| **Security** | Spring Security 6 (BCrypt Password Hashing, Role-Based Access Control, CSRF Protection) |
| **Persistence** | Spring Data JPA / Hibernate ORM 6.4 |
| **Database** | MySQL 8.0+ |
| **Template Engine** | Thymeleaf 3 with Spring Security Dialect |
| **Local AI** | Ollama + Qwen 2.5 3B (local REST API, no cloud dependency) |
| **PDF Generation** | OpenPDF 1.3.30 (A4 Landscape Tables) |
| **File Processing** | Apache POI 5.2.5 (Excel `.xlsx`/`.xls`), OpenCSV 5.9 (CSV) |
| **Real-Time Push** | Server-Sent Events (SSE) via Spring `SseEmitter` |
| **Frontend Styling** | Custom CSS (Responsive CSS Grid & Flexbox, Google Blue Neutral Palette `#1a73e8`) |
| **Frontend Scripts** | Pure Vanilla JavaScript ES6+ (Web Notifications API, SSE Stream, Dynamic Modals) |
| **Testing** | JUnit 5, Spring Boot Test, Spring Security Test, H2 In-Memory DB |
| **Build Tool** | Apache Maven 3.9+ |

---

## 🏛 System Architecture

```text
┌─────────────────────────────────────────────────────────────────────┐
│                        Web Browser (Client)                        │
│  Student Dashboard │ Teacher Dashboard │ Admin Dashboard │ Login   │
└───────────────────────────┬─────────────────────────────────────────┘
                            │ HTTP / SSE
┌───────────────────────────▼─────────────────────────────────────────┐
│                    Spring Boot 3.2.5 (Port 8080)                   │
│ ┌─────────────┐ ┌──────────────┐ ┌───────────────┐ ┌────────────┐ │
│ │ Auth Layer  │ │  Controller  │ │  REST API     │ │ SSE Push   │ │
│ │ Spring Sec  │ │  (Thymeleaf) │ │  (JSON)       │ │ Notifier   │ │
│ └──────┬──────┘ └──────┬───────┘ └───────┬───────┘ └─────┬──────┘ │
│        │               │                 │               │        │
│ ┌──────▼───────────────▼─────────────────▼───────────────▼──────┐ │
│ │                      Service Layer (16 Services)              │ │
│ │  Timetable Generator │ Absence Policy │ Replacement Proposal  │ │
│ │  Timetable Validator │ Substitute Svc │ Notification Service  │ │
│ │  Import Service      │ PDF Service    │ Activity Log Service  │ │
│ └──────────────┬──────────────────────────┬─────────────────────┘ │
│                │                          │                       │
│ ┌──────────────▼──────┐    ┌──────────────▼──────────────┐       │
│ │ Spring Data JPA     │    │ Ollama AI Client            │       │
│ │ 20 Repositories     │    │ REST → localhost:11434      │       │
│ │ 22 JPA Entities     │    │ qwen2.5:3b (local model)    │       │
│ └──────────┬──────────┘    └─────────────────────────────┘       │
└────────────┼─────────────────────────────────────────────────────┘
             │ JDBC
┌────────────▼──────────┐
│  MySQL 8.0 Database   │
│  smart_schedule       │
│  (22 tables)          │
└───────────────────────┘
```

---

## ⚡ Timetable Generation Algorithm

The timetable generator (`TimetableGeneratorService.java`) uses a **constraint-satisfaction heuristic with scoring-based slot selection** and randomized tie-breaking for schedule variety.

### Generation Workflow

1. Fetch all `SubjectAssignment` records for the target section.
2. Validate prerequisites (teacher assignments, period demand vs. capacity).
3. Index existing slot entries; clean any duplicate entries.
4. **Preserve locked slots** (🔒) — never overwrite admin-locked entries.
5. Sort assignments: **Labs first** (require consecutive periods), then by `periodsPerWeek` descending, with random shuffle among equals.
6. **Schedule labs** as consecutive period pairs, avoiding lunch-break splits.
7. **Schedule theory** by evaluating all `(day, period)` combinations with heuristic scoring.
8. Fill remaining periods as **FREE** entries; add **BREAK** and **LUNCH** entries.
9. Run independent hard-constraint validation.
10. Generate AI summary of results.

### Hard Constraints (Never Violated)

| # | Constraint | Description |
|---|---|---|
| 1 | **Faculty Non-Overlapping** | A teacher cannot teach more than one class at the same day & period across **all** sections. |
| 2 | **Classroom Non-Overlapping** | A room/lab cannot host multiple classes simultaneously across all sections. |
| 3 | **Room Capability** | Lab subjects require `COMPUTER_LAB`, `PHYSICS_LAB`, or `ELECTRONICS_LAB` rooms; theory requires `CLASSROOM`. |
| 4 | **Consecutive Lab Slots** | Lab practicals are automatically scheduled back-to-back in consecutive class periods; never split across lunch. |
| 5 | **Locked Slot Preservation** | Admin-locked slots (🔒) are preserved and never overwritten during regeneration. |
| 6 | **Teacher Availability Matrix** | Checks `TeacherAvailability` records to prevent scheduling during declared unavailable hours. |
| 7 | **Daily Workload Limit** | Teachers cannot exceed configurable max daily periods (default: 4). |
| 8 | **Weekly Workload Limit** | Teachers cannot exceed configurable max weekly periods (default: 18). |

### Soft Constraints (Optimized by Heuristic Scoring)

| Constraint | Score Impact |
|---|---|
| **Subject Distribution** | +15 bonus for scheduling on a day the subject hasn't appeared; −20 penalty for same-day duplicate. |
| **Morning Theory Preference** | +5 bonus for periods 1–4 (morning slots). |
| **Daily Class Balance** | −2 per existing class already scheduled on that day (spreads load evenly). |
| **Randomized Tie-Breaking** | +0 to +3 random jitter ensures varied schedules on every regeneration. |

### Post-Generation Validation

An independent `TimetableValidationService` performs 7 hard-constraint checks:
1. Section double-booking
2. Teacher double-booking (within section **and** across all published sections)
3. Teacher availability violations
4. Room double-booking
5. Room capability mismatch
6. Teacher daily workload violations
7. Teacher weekly workload violations

### Generation Results

The system reports one of three statuses:
- **OPTIMAL** — All subjects placed, all constraints satisfied.
- **FEASIBLE** — Most subjects placed, minor soft constraint trade-offs.
- **INFEASIBLE** — Some subjects could not be placed; conflict details reported.

---

## 📋 Absence Management & Policy Enforcement

### Absence Request Workflow

```
Teacher submits request (natural language or form)
        │
        ▼
   AI parses input → Structured data (date, type, reason)
        │
        ▼
   Policy validation (weekly limits, monthly caps, overlap detection)
        │
        ├── Policy OK ──────────────────► Request created (PENDING)
        │                                       │
        └── Policy VIOLATED ────► Request created (PENDING + ESCALATED)
                                        │ Admin notified with violation details
                                        │
                                        ▼
                            Affected classes auto-discovered
                                        │
                                        ▼
                        Replacement plan auto-calculated
                                        │
                                        ▼
                            Admin reviews proposal
                                        │
                          ┌─────────────┼──────────────┐
                          ▼             ▼              ▼
                       Approve      Reject     Override (if violated)
                          │             │              │
                          ▼             ▼              ▼
                  Substitutes     Teacher         Admin approves
                  assigned        notified        with override flag
                  Students &                      recorded in audit
                  teachers notified
```

### Configurable Policy Rules

| Rule | Default | Configurable |
|---|---|---|
| Max absence requests per week (Mon–Sun) | 2 | ✅ via Admin Settings |
| Max leave days per calendar month | 7 | ✅ via Admin Settings |
| Full-day leave count | 1.0 day | — |
| Specific-period leave count | 0.5 day | — |

### Policy Enforcement Details

- **Duplicate/Overlap Detection** — Prevents two active requests (PENDING or APPROVED) for the same date; for specific-period requests, checks individual period overlaps.
- **Weekly Limit** — Counts request *submissions* (not individual days) within a Monday-to-Sunday calendar week.
- **Monthly Limit** — Counts leave *days* (full-day = 1.0, specific-period = 0.5) within a calendar month.
- **Boundary Handling** — Requests spanning two weeks or months are counted in each applicable period.
- **Concurrency Safety** — Database transactions prevent simultaneous submissions from bypassing limits.
- **Admin Escalation** — Violations are never auto-approved; they are flagged with the specific violated rule, relevant counts, and an AI-generated policy briefing.
- **AI Never Decides** — The AI model generates explanations only; it never overrides institutional policy.

---

## 🔄 Automated Substitute Assignment

The `ReplacementProposalService` automatically calculates optimal substitute teachers for every affected class when a teacher becomes unavailable.

### For Each Affected Slot, the System:

1. Retrieves all teachers in the system.
2. Applies **7 hard-constraint filters** (must pass all):
   - Cannot substitute for oneself
   - Not on approved leave on the affected date
   - Available in the teacher availability matrix
   - No conflicting class at that day & period
   - Daily workload limit not exceeded
   - Weekly workload limit not exceeded
   - Must be qualified (teaches the same subject or belongs to the same department)
3. Ranks remaining candidates by **soft scoring**:
   - **+50** — Teaches the exact same subject (direct match)
   - **+40** — Suggested by the absent teacher
   - **+30** — Belongs to the same department as the subject
   - **+(maxWeekly − currentLoad)** — Balanced workload priority (favors less-loaded teachers)
4. Generates an AI-powered explanation of why the top candidate was selected.
5. Tracks simulated workload across all slots in the same absence (prevents double-assigning a teacher to multiple slots).

### Administrator Actions

- **Apply All Replacements** — One-click to accept all proposed substitutions.
- **Manual Override** — Reject proposals and manually assign a different teacher.
- **Cancel Class** — Mark a slot as cancelled if no substitute is available.

All assignments are:
- ✅ Recorded with who initiated, reason, timestamps, and approval decision
- ✅ Notifications sent to affected students and the substitute teacher
- ✅ Logged in the audit activity stream

---

## 🔔 Real-Time Notification System

| Feature | Implementation |
|---|---|
| **Delivery** | Server-Sent Events (SSE) via Spring `SseEmitter` with 30-minute timeout |
| **Storage** | Persistent in-database `Notification` entity per user |
| **Live Push** | `ConcurrentHashMap<UserId, SseEmitter>` for instant browser delivery |
| **Bell Indicator** | Unread count badge + dropdown with recent notifications |
| **Mark as Read** | Individual or bulk "Mark All as Read" |

### 8 Notification Types

| Type | Trigger |
|---|---|
| `CLASS_CANCELLATION` | Admin cancels a class slot |
| `TIMETABLE_CHANGE` | Published timetable updated |
| `ABSENCE_APPROVED` | Teacher absence request approved |
| `ABSENCE_REJECTED` | Teacher absence request rejected |
| `SUBSTITUTE_ASSIGNED` | Teacher assigned as substitute |
| `ANNOUNCEMENT` | New campus announcement published |
| `CLASS_REMINDER` | 5-minute countdown bell (browser Web Notifications API) |
| `SYSTEM` | System-generated events |

### Targeted Notification Methods

- `notifyStudentsInSection(section, ...)` — All students in a specific section
- `notifyTeacher(teacher, ...)` — Individual teacher
- `notifyAdmins(...)` — All users with `ROLE_ADMIN`

---

## 📥 Batch Import Specifications

The application supports importing data in **CSV** and **Microsoft Excel** (`.xlsx`, `.xls`) formats with row-level validation and detailed error reporting.

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

> **Tip**: Download pre-formatted templates directly from the application at **Admin → Import → Download Template**.

---

## 🔑 Demo User Accounts

The system automatically initializes seed data on startup via `DataInitializer.java`:

| Role | Username / ID | Password | Details |
|---|---|---|---|
| **Admin** | `admin` | `admin123` | Full system access |
| **Teacher** | `T001` | `teacher123` | Dr. Kumar (CSE) |
| **Teacher** | `T002` | `teacher123` | Prof. Sharma (CSE) |
| **Teacher** | `T003` – `T015` | `teacher123` | Faculty across CSE, ECE, EEE, Civil, Mech |
| **Student** | `S001` | `student123` | CSE-A, 2nd Year, Semester 3 |
| **Student** | `S002` | `student123` | CSE-A, 2nd Year, Semester 3 |
| **Student** | `S001` – `S030` | `student123` | Students across all 5 sections |

### Seed Data Summary

| Entity | Count | Details |
|---|---|---|
| Departments | 5 | CSE, ECE, EEE, Civil, Mechanical |
| Academic Years | 4 | 1st through 4th Year |
| Sections | 5 | CSE-A, CSE-B, ECE-A, ECE-B, EEE-A (all 2nd Year) |
| Periods | 10 | 6 CLASS (9:00–15:30) + 3 BREAK + 1 LUNCH (12:50–13:40) |
| Classrooms | 12 | 7 Lecture Rooms, 2 CS Labs, 1 Electronics Lab, 1 Physics Lab, 1 Seminar Hall |
| Teachers | 15 | T001–T015 across all departments |
| Students | 30 | S001–S030 across 5 sections |
| Subjects | 24 | 8 CSE + 8 ECE + 8 EEE (theory + labs each) |
| Subject Assignments | Complete | All 5 sections fully assigned with faculty |
| Published Timetable | 1 | CSE-A Mon–Fri pre-built schedule |

---

## 🚀 Getting Started & Setup

### Prerequisites

- **Java 21 JDK** or newer installed
- **MySQL Server 8.0+** running locally
- **Apache Maven 3.9+** installed
- **Ollama** (optional, for AI features) — [Download here](https://ollama.com/download)

### 1. Database Configuration

Create the MySQL database:
```sql
CREATE DATABASE smart_schedule CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Update `src/main/resources/application.properties` if your MySQL credentials differ:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/smart_schedule?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=root
```

### 2. Set Up Local AI (Optional but Recommended)

Install Ollama from [https://ollama.com/download](https://ollama.com/download), then pull the model:
```bash
ollama pull qwen2.5:3b
```

Verify Ollama is running:
```bash
curl http://localhost:11434/api/tags
```

> **Note**: If Ollama is not installed, the application will still work — all AI features gracefully fall back to rule-based processing.

### 3. Build and Run

Build the project:
```bash
mvn clean package
```

Run the application:
```bash
mvn spring-boot:run
```

Or run the packaged JAR:
```bash
java -jar target/smart-schedule-assist-1.0.0.jar
```

### 4. Access the Application

Open your browser and navigate to:
```
http://localhost:8080
```

Sign in with any of the demo accounts listed above. The login page provides separate tabs for **Student**, **Teacher**, and **Admin** roles with cross-role validation.

### 5. Quick Start Guide

1. **As Admin**: Go to **Timetables → Manage** → Select a section → Click **Auto-Generate Timetable** → Review → **Publish**.
2. **As Teacher**: Go to **Absence Management** → Type a natural language absence request or fill the form → Submit.
3. **As Admin**: Go to **Absence Requests** → Review the request → Click **Calculate Replacements** → **Apply Replacements** → **Approve**.
4. **As Student**: Check your **Dashboard** for real-time class schedule and notifications.

---

## ⚙️ Configuration Reference

### Application Properties (`application.properties`)

| Property | Default | Description |
|---|---|---|
| `server.port` | `8080` | HTTP server port |
| `server.address` | `0.0.0.0` | Bind address (all interfaces for LAN access) |
| `spring.datasource.url` | `jdbc:mysql://localhost:3306/smart_schedule...` | MySQL connection URL |
| `spring.datasource.username` | `root` | Database username (overridable via `DB_USERNAME` env var) |
| `spring.datasource.password` | `your_password` | Database password (overridable via `DB_PASSWORD` env var) |
| `spring.jpa.hibernate.ddl-auto` | `update` | Schema management (auto-creates/updates tables) |
| `spring.servlet.multipart.max-file-size` | `10MB` | Max upload file size for batch imports |
| `server.servlet.session.timeout` | `30m` | Session timeout |
| `ollama.api.url` | `http://localhost:11434` | Ollama API base URL |
| `ollama.model` | `qwen2.5:3b` | Default AI model for inference |
| `spring.thymeleaf.cache` | `false` | Template caching (set `true` in production) |

### Runtime System Settings (Admin → Settings)

| Setting Key | Default | Description |
|---|---|---|
| `current_semester` | `3` | Active semester number |
| `academic_year` | `2026-2027` | Academic year label |
| `max_weekly_absence_requests` | `2` | Max absence request submissions per Mon–Sun week |
| `max_monthly_leave_days` | `7` | Max leave days per calendar month |
| `max_daily_teacher_periods` | `4` | Max teaching periods per day per teacher |
| `max_weekly_teacher_periods` | `18` | Max teaching periods per week per teacher |

---

## 🧪 Testing

Run the full test suite:
```bash
mvn test
```

### Test Classes (8 total)

| Test Class | What It Validates |
|---|---|
| `SmartScheduleApplicationTests` | Spring application context loads successfully |
| `TimetableGeneratorServiceTest` | Constraint satisfaction, period placement, collision avoidance |
| `TimetableValidationServiceTest` | Independent hard-constraint validation (all 7 checks) |
| `AbsenceServiceTest` | Affected class auto-discovery, approval lifecycle |
| `AbsencePolicyServiceTest` | Weekly/monthly policy limit validation, duplicate detection |
| `SubstituteServiceTest` | Substitute teacher conflict checking, student notification dispatch |
| `ReplacementProposalServiceTest` | Automated replacement plan calculation and ranking |
| `OllamaServiceTest` | AI service availability check, natural language parsing |

Tests use **H2 in-memory database** for isolation and do not require MySQL or Ollama to be running.

---

## 📂 Project Architecture

```text
com.smartschedule
├── config/                     # SecurityConfig, WebConfig, DataInitializer
├── controller/                 # AuthController, ApiController, StudentController,
│   │                           #   TeacherController, AdminController,
│   │                           #   NotificationRestController, TimetablePdfController
│   └── admin/                  # TimetableController, AbsenceManagementController,
│                               #   SubstituteController, TimetableRequestController,
│                               #   AnnouncementController, StudentManagementController,
│                               #   TeacherManagementController, SubjectController,
│                               #   ClassroomController, DepartmentController,
│                               #   SectionController, SettingsController,
│                               #   ActivityLogController, ImportController
├── dto/                        # StudentDTO, TeacherDTO, TimetableEntryDTO,
│                               #   ImportResult, GenerationResult, PolicyCheckResult, etc.
├── exception/                  # GlobalExceptionHandler, ResourceNotFoundException, etc.
├── model/                      # 22 JPA Entities
│   │   User, Student, Teacher, Department, AcademicYear, Section,
│   │   Subject, SubjectAssignment, Classroom, Period,
│   │   TimetableEntry, AbsenceRequest, AbsenceAffectedClass,
│   │   SubstituteAssignment, TimetableChangeRequest,
│   │   Notification, Announcement, AnnouncementTarget,
│   │   ActivityLog, SystemSettings, TeacherAvailability,
│   │   PasswordResetToken
│   └── enums/                  # 11 Enums
│       UserRole, SubjectType, RoomType, PeriodType,
│       EntryStatus, RequestStatus, AbsenceType, ChangeType,
│       ClassResolution, NotificationType, TargetType
├── repository/                 # 20 Spring Data JPA Repositories
├── security/                   # CustomAuthenticationProvider, CustomUserDetailsService,
│                               #   LoginSuccessHandler
└── service/                    # 17 Service Classes
    TimetableGeneratorService, TimetableService, TimetableValidationService,
    AbsenceService, AbsencePolicyService, SubstituteService,
    ReplacementProposalService, OllamaService, NotificationService,
    AnnouncementService, ImportService, PdfService, ActivityLogService,
    StudentService, TeacherService, SystemSettingsService,
    PasswordResetService
```

### Template Structure

```text
src/main/resources/templates/
├── admin/                      # 17+ admin pages (dashboard, timetable editor, absence
│                               #   management, CRUD pages, settings, imports, etc.)
├── auth/                       # 3 pages (login, forgot-password, reset-password)
├── student/                    # 6 pages (dashboard, timetable, notifications, profile, etc.)
├── teacher/                    # 7 pages (dashboard, timetable, absence, change-requests, etc.)
├── fragments/                  # 3 reusable fragments (layout, header, sidebar)
└── error/                      # 3 error pages (403, 404, 500)
```

---

## 🌐 API Endpoints

### REST API (JSON)

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `GET` | `/api/timetable/next-class` | Any | Next upcoming class for current user |
| `GET` | `/api/teachers/available?dayOfWeek=&periodId=` | Admin | Available substitute teachers for a slot |
| `GET` | `/api/ai/status` | Any | Ollama AI status, available models, configured model |
| `POST` | `/api/ai/parse-absence` | Teacher+ | Parse natural language absence request via AI |
| `GET` | `/api/policy/check?date=&type=` | Teacher | Check absence policy limits before submission |

### Notification API (JSON + SSE)

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `GET` | `/api/notifications/stream` | Any | SSE real-time notification stream |
| `GET` | `/api/notifications/unread-count` | Any | Unread notification count |
| `GET` | `/api/notifications/recent` | Any | Last 10 notifications with relative timestamps |
| `POST` | `/api/notifications/{id}/read` | Any | Mark notification as read |
| `POST` | `/api/notifications/read-all` | Any | Mark all notifications as read |

### PDF Export Endpoints

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `GET` | `/student/timetable/pdf` | Student | Download section timetable PDF |
| `GET` | `/student/timetable/pdf/today` | Student | Download today's timetable PDF |
| `GET` | `/teacher/timetable/pdf` | Teacher | Download teaching schedule PDF |
| `GET` | `/admin/timetables/pdf/{sectionId}` | Admin | Download section timetable PDF |
| `GET` | `/admin/timetables/pdf/teacher/{teacherId}` | Admin | Download teacher schedule PDF |
| `GET` | `/admin/timetables/pdf/department/{deptId}` | Admin | Download department timetable PDF |

---

## 🖼 Screenshots & UI

The application features a modern, responsive UI with:

- **Multi-role login page** with Student / Teacher / Admin tabs and cross-role validation
- **Animated dashboards** with live stat cards, next-class countdowns, and activity feeds
- **Interactive timetable grids** with color-coded status badges (Active, Free, Cancelled, Substitute)
- **Dark mode** toggle with persistent preference
- **Mobile-responsive** layouts optimized for tablets and phones

---

## 📄 License

This project is developed for educational and institutional use.

---

<p align="center">
  <b>Smart Schedule Assist v1.0.0</b> — AI-Powered College Timetable Management<br>
  Built with ❤️ using Spring Boot, Thymeleaf, MySQL & Ollama AI
</p>
