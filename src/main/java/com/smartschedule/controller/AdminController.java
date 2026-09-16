package com.smartschedule.controller;

import com.smartschedule.dto.PasswordChangeDTO;
import com.smartschedule.model.*;
import com.smartschedule.model.enums.EntryStatus;
import com.smartschedule.model.enums.RequestStatus;
import com.smartschedule.repository.*;
import com.smartschedule.service.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final StudentService studentService;
    private final TeacherService teacherService;
    private final DepartmentRepository departmentRepository;
    private final SectionRepository sectionRepository;
    private final SubjectRepository subjectRepository;
    private final ClassroomRepository classroomRepository;
    private final AbsenceService absenceService;
    private final TimetableService timetableService;
    private final ActivityLogService activityLogService;
    private final NotificationService notificationService;
    private final TimetableChangeRequestRepository timetableChangeRequestRepository;
    private final TimetableEntryRepository timetableEntryRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public AdminController(UserRepository userRepository,
                           StudentService studentService,
                           TeacherService teacherService,
                           DepartmentRepository departmentRepository,
                           SectionRepository sectionRepository,
                           SubjectRepository subjectRepository,
                           ClassroomRepository classroomRepository,
                           AbsenceService absenceService,
                           TimetableService timetableService,
                           ActivityLogService activityLogService,
                           NotificationService notificationService,
                           TimetableChangeRequestRepository timetableChangeRequestRepository,
                           TimetableEntryRepository timetableEntryRepository,
                           StudentRepository studentRepository,
                           TeacherRepository teacherRepository,
                           BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.studentService = studentService;
        this.teacherService = teacherService;
        this.departmentRepository = departmentRepository;
        this.sectionRepository = sectionRepository;
        this.subjectRepository = subjectRepository;
        this.classroomRepository = classroomRepository;
        this.absenceService = absenceService;
        this.timetableService = timetableService;
        this.activityLogService = activityLogService;
        this.notificationService = notificationService;
        this.timetableChangeRequestRepository = timetableChangeRequestRepository;
        this.timetableEntryRepository = timetableEntryRepository;
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.passwordEncoder = passwordEncoder;
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByUsername(auth.getName()).orElseThrow();
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        User user = getCurrentUser();
        model.addAttribute("studentCount", studentService.countStudents());
        model.addAttribute("teacherCount", teacherService.countTeachers());
        model.addAttribute("departmentCount", departmentRepository.count());
        model.addAttribute("sectionCount", sectionRepository.count());
        model.addAttribute("subjectCount", subjectRepository.count());
        model.addAttribute("classroomCount", classroomRepository.count());
        model.addAttribute("pendingAbsences", absenceService.countPendingAbsences());
        model.addAttribute("pendingChangeRequests", timetableChangeRequestRepository.countByStatus(RequestStatus.PENDING));
        model.addAttribute("recentActivity", activityLogService.getRecentActivity(8));
        model.addAttribute("todaysCancelledCount", timetableEntryRepository.countByStatusAndDayOfWeekAndPublishedTrue(
                EntryStatus.CANCELLED, LocalDate.now().getDayOfWeek()));
        model.addAttribute("unreadCount", notificationService.getUnreadCount(user));

        return "admin/dashboard";
    }

    @GetMapping("/notifications")
    public String notifications(Model model) {
        User user = getCurrentUser();
        model.addAttribute("notifications", notificationService.getUserNotifications(user));
        model.addAttribute("unreadCount", notificationService.getUnreadCount(user));
        return "admin/notifications";
    }

    @GetMapping("/search")
    public String search(@RequestParam(value = "q", defaultValue = "") String query, Model model) {
        User user = getCurrentUser();
        String q = query.trim().toLowerCase();

        List<Student> students = studentRepository.findAll().stream()
                .filter(s -> s.getStudentId().toLowerCase().contains(q) ||
                        (s.getUser() != null && s.getUser().getFullName().toLowerCase().contains(q)))
                .collect(Collectors.toList());

        List<Teacher> teachers = teacherRepository.findAll().stream()
                .filter(t -> t.getTeacherId().toLowerCase().contains(q) ||
                        (t.getUser() != null && t.getUser().getFullName().toLowerCase().contains(q)))
                .collect(Collectors.toList());

        List<Subject> subjects = subjectRepository.findAll().stream()
                .filter(s -> s.getName().toLowerCase().contains(q) || s.getCode().toLowerCase().contains(q))
                .collect(Collectors.toList());

        List<Section> sections = sectionRepository.findAll().stream()
                .filter(s -> s.getDisplayName().toLowerCase().contains(q))
                .collect(Collectors.toList());

        List<Department> departments = departmentRepository.findAll().stream()
                .filter(d -> d.getName().toLowerCase().contains(q) || d.getCode().toLowerCase().contains(q))
                .collect(Collectors.toList());

        List<Classroom> classrooms = classroomRepository.findAll().stream()
                .filter(c -> c.getName().toLowerCase().contains(q))
                .collect(Collectors.toList());

        model.addAttribute("query", query);
        model.addAttribute("students", students);
        model.addAttribute("teachers", teachers);
        model.addAttribute("subjects", subjects);
        model.addAttribute("sections", sections);
        model.addAttribute("departments", departments);
        model.addAttribute("classrooms", classrooms);
        model.addAttribute("unreadCount", notificationService.getUnreadCount(user));

        return "admin/search";
    }

    @GetMapping("/change-password")
    public String changePasswordForm(Model model) {
        User user = getCurrentUser();
        model.addAttribute("unreadCount", notificationService.getUnreadCount(user));
        return "admin/change-password";
    }

    @PostMapping("/change-password")
    public String changePassword(@ModelAttribute PasswordChangeDTO dto, RedirectAttributes redirectAttributes) {
        User user = getCurrentUser();
        if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Current password is incorrect.");
            return "redirect:/admin/change-password";
        }
        if (dto.getNewPassword() == null || dto.getNewPassword().trim().length() < 4) {
            redirectAttributes.addFlashAttribute("errorMessage", "New password must be at least 4 characters.");
            return "redirect:/admin/change-password";
        }
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            redirectAttributes.addFlashAttribute("errorMessage", "New passwords do not match.");
            return "redirect:/admin/change-password";
        }
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);
        redirectAttributes.addFlashAttribute("successMessage", "Password changed successfully.");
        return "redirect:/admin/dashboard";
    }
}
