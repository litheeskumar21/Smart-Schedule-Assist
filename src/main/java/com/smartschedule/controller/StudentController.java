package com.smartschedule.controller;

import com.smartschedule.dto.PasswordChangeDTO;
import com.smartschedule.model.Period;
import com.smartschedule.model.Student;
import com.smartschedule.model.TimetableEntry;
import com.smartschedule.model.User;
import com.smartschedule.repository.PeriodRepository;
import com.smartschedule.repository.UserRepository;
import com.smartschedule.service.AnnouncementService;
import com.smartschedule.service.NotificationService;
import com.smartschedule.service.StudentService;
import com.smartschedule.service.TimetableService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/student")
public class StudentController {

    private final StudentService studentService;
    private final TimetableService timetableService;
    private final NotificationService notificationService;
    private final AnnouncementService announcementService;
    private final UserRepository userRepository;
    private final PeriodRepository periodRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public StudentController(StudentService studentService,
                             TimetableService timetableService,
                             NotificationService notificationService,
                             AnnouncementService announcementService,
                             UserRepository userRepository,
                             PeriodRepository periodRepository,
                             BCryptPasswordEncoder passwordEncoder) {
        this.studentService = studentService;
        this.timetableService = timetableService;
        this.notificationService = notificationService;
        this.announcementService = announcementService;
        this.userRepository = userRepository;
        this.periodRepository = periodRepository;
        this.passwordEncoder = passwordEncoder;
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByUsername(auth.getName()).orElseThrow();
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        User user = getCurrentUser();
        Student student = studentService.getStudentByUser(user);
        DayOfWeek currentDay = LocalDate.now().getDayOfWeek();

        List<TimetableEntry> todayEntries = student.getSection() != null ?
                timetableService.getTimetableBySectionAndDay(student.getSection(), currentDay) : List.of();
        TimetableEntry nextClass = student.getSection() != null ?
                timetableService.getNextClass(student.getSection()) : null;
        Map<DayOfWeek, List<TimetableEntry>> weekly = student.getSection() != null ?
                timetableService.getWeeklyTimetable(student.getSection()) : Map.of();

        model.addAttribute("student", student);
        model.addAttribute("todayEntries", todayEntries);
        model.addAttribute("nextClass", nextClass);
        model.addAttribute("allPeriods", timetableService.getAllPeriods());
        model.addAttribute("weeklyTimetable", weekly);
        model.addAttribute("currentDay", currentDay);
        model.addAttribute("unreadCount", notificationService.getUnreadCount(user));

        return "student/dashboard";
    }

    @GetMapping("/timetable")
    public String timetable(Model model) {
        User user = getCurrentUser();
        Student student = studentService.getStudentByUser(user);
        DayOfWeek currentDay = LocalDate.now().getDayOfWeek();

        Map<DayOfWeek, List<TimetableEntry>> weekly = student.getSection() != null ?
                timetableService.getWeeklyTimetable(student.getSection()) : Map.of();

        model.addAttribute("student", student);
        model.addAttribute("weeklyTimetable", weekly);
        model.addAttribute("allPeriods", timetableService.getAllPeriods());
        model.addAttribute("currentDay", currentDay);
        model.addAttribute("unreadCount", notificationService.getUnreadCount(user));

        return "student/timetable";
    }

    @GetMapping("/timetable/day/{dayName}")
    public String timetableDay(@PathVariable String dayName, Model model) {
        User user = getCurrentUser();
        Student student = studentService.getStudentByUser(user);
        DayOfWeek selectedDay;
        try {
            selectedDay = DayOfWeek.valueOf(dayName.toUpperCase());
        } catch (IllegalArgumentException e) {
            selectedDay = LocalDate.now().getDayOfWeek();
        }

        List<TimetableEntry> dayEntries = student.getSection() != null ?
                timetableService.getTimetableBySectionAndDay(student.getSection(), selectedDay) : List.of();

        model.addAttribute("student", student);
        model.addAttribute("dayEntries", dayEntries);
        model.addAttribute("allPeriods", timetableService.getAllPeriods());
        model.addAttribute("selectedDay", selectedDay);
        model.addAttribute("unreadCount", notificationService.getUnreadCount(user));

        return "student/timetable";
    }

    @GetMapping("/notifications")
    public String notifications(Model model) {
        User user = getCurrentUser();
        model.addAttribute("notifications", notificationService.getUserNotifications(user));
        model.addAttribute("unreadCount", notificationService.getUnreadCount(user));
        return "student/notifications";
    }

    @GetMapping("/announcements")
    public String announcements(Model model) {
        User user = getCurrentUser();
        model.addAttribute("announcements", announcementService.getAnnouncementsForUser(user));
        model.addAttribute("unreadCount", notificationService.getUnreadCount(user));
        return "student/announcements";
    }

    @GetMapping("/profile")
    public String profile(Model model) {
        User user = getCurrentUser();
        Student student = studentService.getStudentByUser(user);
        model.addAttribute("student", student);
        model.addAttribute("unreadCount", notificationService.getUnreadCount(user));
        return "student/profile";
    }

    @GetMapping("/change-password")
    public String changePasswordForm(Model model) {
        User user = getCurrentUser();
        model.addAttribute("unreadCount", notificationService.getUnreadCount(user));
        return "student/change-password";
    }

    @PostMapping("/change-password")
    public String changePassword(@ModelAttribute PasswordChangeDTO dto, RedirectAttributes redirectAttributes) {
        User user = getCurrentUser();

        if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Current password is incorrect.");
            return "redirect:/student/change-password";
        }

        if (dto.getNewPassword() == null || dto.getNewPassword().trim().length() < 4) {
            redirectAttributes.addFlashAttribute("errorMessage", "New password must be at least 4 characters.");
            return "redirect:/student/change-password";
        }

        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            redirectAttributes.addFlashAttribute("errorMessage", "New passwords do not match.");
            return "redirect:/student/change-password";
        }

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);

        redirectAttributes.addFlashAttribute("successMessage", "Password changed successfully.");
        return "redirect:/student/profile";
    }
}
