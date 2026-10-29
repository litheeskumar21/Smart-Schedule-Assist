package com.smartschedule.controller;

import com.smartschedule.dto.AbsenceRequestDTO;
import com.smartschedule.dto.PasswordChangeDTO;
import com.smartschedule.dto.TimetableChangeRequestDTO;
import com.smartschedule.model.*;
import com.smartschedule.model.enums.ChangeType;
import com.smartschedule.model.enums.NotificationType;
import com.smartschedule.model.enums.RequestStatus;
import com.smartschedule.repository.PeriodRepository;
import com.smartschedule.repository.TeacherRepository;
import com.smartschedule.repository.TimetableChangeRequestRepository;
import com.smartschedule.repository.TimetableEntryRepository;
import com.smartschedule.repository.UserRepository;
import com.smartschedule.service.*;
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
@RequestMapping("/teacher")
public class TeacherController {

    private final TeacherService teacherService;
    private final TimetableService timetableService;
    private final AbsenceService absenceService;
    private final NotificationService notificationService;
    private final AnnouncementService announcementService;
    private final UserRepository userRepository;
    private final PeriodRepository periodRepository;
    private final TeacherRepository teacherRepository;
    private final TimetableChangeRequestRepository timetableChangeRequestRepository;
    private final TimetableEntryRepository timetableEntryRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public TeacherController(TeacherService teacherService,
                             TimetableService timetableService,
                             AbsenceService absenceService,
                             NotificationService notificationService,
                             AnnouncementService announcementService,
                             UserRepository userRepository,
                             PeriodRepository periodRepository,
                             TeacherRepository teacherRepository,
                             TimetableChangeRequestRepository timetableChangeRequestRepository,
                             TimetableEntryRepository timetableEntryRepository,
                             BCryptPasswordEncoder passwordEncoder) {
        this.teacherService = teacherService;
        this.timetableService = timetableService;
        this.absenceService = absenceService;
        this.notificationService = notificationService;
        this.announcementService = announcementService;
        this.userRepository = userRepository;
        this.periodRepository = periodRepository;
        this.teacherRepository = teacherRepository;
        this.timetableChangeRequestRepository = timetableChangeRequestRepository;
        this.timetableEntryRepository = timetableEntryRepository;
        this.passwordEncoder = passwordEncoder;
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByUsername(auth.getName()).orElseThrow();
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        User user = getCurrentUser();
        Teacher teacher = teacherService.getTeacherByUser(user);
        DayOfWeek currentDay = LocalDate.now().getDayOfWeek();

        List<TimetableEntry> todayEntries = timetableService.getTimetableByTeacherAndDay(teacher, currentDay);
        TimetableEntry nextClass = timetableService.getNextClassForTeacher(teacher);
        Map<DayOfWeek, List<TimetableEntry>> weekly = timetableService.getWeeklyTeacherTimetable(teacher);

        model.addAttribute("teacher", teacher);
        model.addAttribute("todayEntries", todayEntries);
        model.addAttribute("nextClass", nextClass);
        model.addAttribute("allPeriods", timetableService.getAllPeriods());
        model.addAttribute("weeklyTimetable", weekly);
        model.addAttribute("currentDay", currentDay);
        model.addAttribute("unreadCount", notificationService.getUnreadCount(user));

        return "teacher/dashboard";
    }

    @GetMapping("/timetable")
    public String timetable(Model model) {
        User user = getCurrentUser();
        Teacher teacher = teacherService.getTeacherByUser(user);
        DayOfWeek currentDay = LocalDate.now().getDayOfWeek();

        Map<DayOfWeek, List<TimetableEntry>> weekly = timetableService.getWeeklyTeacherTimetable(teacher);

        model.addAttribute("teacher", teacher);
        model.addAttribute("weeklyTimetable", weekly);
        model.addAttribute("allPeriods", timetableService.getAllPeriods());
        model.addAttribute("currentDay", currentDay);
        model.addAttribute("unreadCount", notificationService.getUnreadCount(user));

        return "teacher/timetable";
    }

    @GetMapping("/timetable/day/{dayName}")
    public String timetableDay(@PathVariable String dayName, Model model) {
        User user = getCurrentUser();
        Teacher teacher = teacherService.getTeacherByUser(user);
        DayOfWeek selectedDay;
        try {
            selectedDay = DayOfWeek.valueOf(dayName.toUpperCase());
        } catch (IllegalArgumentException e) {
            selectedDay = LocalDate.now().getDayOfWeek();
        }

        List<TimetableEntry> dayEntries = timetableService.getTimetableByTeacherAndDay(teacher, selectedDay);

        model.addAttribute("teacher", teacher);
        model.addAttribute("dayEntries", dayEntries);
        model.addAttribute("allPeriods", timetableService.getAllPeriods());
        model.addAttribute("selectedDay", selectedDay);
        model.addAttribute("unreadCount", notificationService.getUnreadCount(user));

        return "teacher/timetable";
    }

    @GetMapping("/absence")
    public String absence(Model model) {
        User user = getCurrentUser();
        Teacher teacher = teacherService.getTeacherByUser(user);

        model.addAttribute("teacher", teacher);
        model.addAttribute("absenceRequests", absenceService.getTeacherAbsences(teacher));
        model.addAttribute("allPeriods", timetableService.getClassPeriods());
        model.addAttribute("allTeachers", teacherRepository.findAll());
        model.addAttribute("unreadCount", notificationService.getUnreadCount(user));

        return "teacher/absence";
    }

    @PostMapping("/absence")
    public String submitAbsence(@ModelAttribute AbsenceRequestDTO dto, RedirectAttributes redirectAttributes) {
        User user = getCurrentUser();
        Teacher teacher = teacherService.getTeacherByUser(user);
        try {
            absenceService.submitAbsence(dto, teacher);
            redirectAttributes.addFlashAttribute("successMessage", "Absence request submitted successfully. Pending admin review.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error submitting absence request: " + e.getMessage());
        }
        return "redirect:/teacher/absence";
    }

    @PostMapping("/absence/{id}/withdraw")
    public String withdrawAbsence(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        User user = getCurrentUser();
        Teacher teacher = teacherService.getTeacherByUser(user);
        try {
            absenceService.withdrawAbsence(id, teacher);
            redirectAttributes.addFlashAttribute("successMessage", "Absence request withdrawn successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error withdrawing absence request: " + e.getMessage());
        }
        return "redirect:/teacher/absence";
    }

    @GetMapping("/change-requests")
    public String changeRequests(Model model) {
        User user = getCurrentUser();
        Teacher teacher = teacherService.getTeacherByUser(user);

        model.addAttribute("teacher", teacher);
        model.addAttribute("changeRequests", timetableChangeRequestRepository.findByTeacherOrderByCreatedAtDesc(teacher));
        model.addAttribute("myEntries", timetableService.getTimetableByTeacher(teacher));
        model.addAttribute("unreadCount", notificationService.getUnreadCount(user));

        return "teacher/change-requests";
    }

    @PostMapping("/change-request")
    public String submitChangeRequest(@ModelAttribute TimetableChangeRequestDTO dto, RedirectAttributes redirectAttributes) {
        User user = getCurrentUser();
        Teacher teacher = teacherService.getTeacherByUser(user);

        TimetableEntry entry = timetableEntryRepository.findById(dto.getTimetableEntryId()).orElse(null);
        if (entry == null || !entry.getTeacher().getId().equals(teacher.getId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "You can only request changes for your own assigned classes.");
            return "redirect:/teacher/change-requests";
        }

        TimetableChangeRequest request = new TimetableChangeRequest();
        request.setTeacher(teacher);
        request.setTimetableEntry(entry);
        request.setChangeType(dto.getChangeType() != null ? dto.getChangeType() : ChangeType.MODIFY);
        request.setRequestedChanges(dto.getRequestedChanges());
        request.setReason(dto.getReason());
        request.setStatus(RequestStatus.PENDING);
        timetableChangeRequestRepository.save(request);

        // Notify admins about new timetable change request
        String teacherName = teacher.getUser() != null ? teacher.getUser().getFullName() : "Faculty";
        String slotInfo = entry.getDayOfWeek() + " Period " + (entry.getPeriod() != null ? entry.getPeriod().getPeriodNumber() : "");
        String subName = entry.getSubject() != null ? entry.getSubject().getName() : "class";
        String prop = dto.getRequestedChanges() != null && !dto.getRequestedChanges().trim().isEmpty() ? " Proposed: " + dto.getRequestedChanges() : "";
        notificationService.notifyAdmins(
                "New Timetable Change Request: " + teacherName,
                teacherName + " requested to modify " + subName + " (" + slotInfo + ")." + prop,
                NotificationType.TIMETABLE_CHANGE
        );

        redirectAttributes.addFlashAttribute("successMessage", "Timetable change request submitted. Pending admin review.");
        return "redirect:/teacher/change-requests";
    }

    @PostMapping("/cancel-request")
    public String cancelRequest(@RequestParam Long timetableEntryId, @RequestParam String reason, RedirectAttributes redirectAttributes) {
        User user = getCurrentUser();
        Teacher teacher = teacherService.getTeacherByUser(user);

        TimetableEntry entry = timetableEntryRepository.findById(timetableEntryId).orElse(null);
        if (entry == null || !entry.getTeacher().getId().equals(teacher.getId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "You can only request cancellation for your own classes.");
            return "redirect:/teacher/change-requests";
        }

        TimetableChangeRequest request = new TimetableChangeRequest();
        request.setTeacher(teacher);
        request.setTimetableEntry(entry);
        request.setChangeType(ChangeType.CANCEL);
        request.setReason(reason);
        request.setRequestedChanges("Class Cancellation Request");
        request.setStatus(RequestStatus.PENDING);
        timetableChangeRequestRepository.save(request);

        // Notify admins about cancellation request
        String teacherName = teacher.getUser() != null ? teacher.getUser().getFullName() : "Faculty";
        String slotInfo = entry.getDayOfWeek() + " Period " + (entry.getPeriod() != null ? entry.getPeriod().getPeriodNumber() : "");
        String subName = entry.getSubject() != null ? entry.getSubject().getName() : "class";
        notificationService.notifyAdmins(
                "Class Cancellation Request: " + teacherName,
                teacherName + " requested cancellation for " + subName + " (" + slotInfo + "). Reason: " + reason,
                NotificationType.CLASS_CANCELLATION
        );

        redirectAttributes.addFlashAttribute("successMessage", "Cancellation request submitted. Pending admin review.");
        return "redirect:/teacher/change-requests";
    }

    @GetMapping("/notifications")
    public String notifications(Model model) {
        User user = getCurrentUser();
        model.addAttribute("notifications", notificationService.getUserNotifications(user));
        model.addAttribute("unreadCount", notificationService.getUnreadCount(user));
        return "teacher/notifications";
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
        Teacher teacher = teacherService.getTeacherByUser(user);
        model.addAttribute("teacher", teacher);
        model.addAttribute("unreadCount", notificationService.getUnreadCount(user));
        return "teacher/profile";
    }

    @GetMapping("/change-password")
    public String changePasswordForm(Model model) {
        User user = getCurrentUser();
        model.addAttribute("unreadCount", notificationService.getUnreadCount(user));
        return "teacher/change-password";
    }

    @PostMapping("/change-password")
    public String changePassword(@ModelAttribute PasswordChangeDTO dto, RedirectAttributes redirectAttributes) {
        User user = getCurrentUser();

        if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Current password is incorrect.");
            return "redirect:/teacher/change-password";
        }

        if (dto.getNewPassword() == null || dto.getNewPassword().trim().length() < 4) {
            redirectAttributes.addFlashAttribute("errorMessage", "New password must be at least 4 characters.");
            return "redirect:/teacher/change-password";
        }

        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            redirectAttributes.addFlashAttribute("errorMessage", "New passwords do not match.");
            return "redirect:/teacher/change-password";
        }

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);

        redirectAttributes.addFlashAttribute("successMessage", "Password changed successfully.");
        return "redirect:/teacher/profile";
    }
}
