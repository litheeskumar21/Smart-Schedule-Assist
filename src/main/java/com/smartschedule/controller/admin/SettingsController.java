package com.smartschedule.controller.admin;

import com.smartschedule.model.User;
import com.smartschedule.service.ActivityLogService;
import com.smartschedule.service.NotificationService;
import com.smartschedule.service.SystemSettingsService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalTime;

@Controller
@RequestMapping("/admin/settings")
public class SettingsController {

    private final SystemSettingsService systemSettingsService;
    private final ActivityLogService activityLogService;
    private final NotificationService notificationService;
    private final com.smartschedule.repository.UserRepository userRepository;

    public SettingsController(SystemSettingsService systemSettingsService,
                              ActivityLogService activityLogService,
                              NotificationService notificationService,
                              com.smartschedule.repository.UserRepository userRepository) {
        this.systemSettingsService = systemSettingsService;
        this.activityLogService = activityLogService;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByUsername(auth.getName()).orElseThrow();
    }

    @GetMapping
    public String viewSettings(Model model) {
        User user = getCurrentUser();
        model.addAttribute("currentSemester", systemSettingsService.getCurrentSemester());
        model.addAttribute("academicYear", systemSettingsService.getAcademicYear());
        model.addAttribute("periods", systemSettingsService.getAllPeriods());
        model.addAttribute("unreadCount", notificationService.getUnreadCount(user));
        return "admin/settings";
    }

    @PostMapping
    public String updateSettings(@RequestParam String currentSemester,
                                 @RequestParam String academicYear,
                                 RedirectAttributes redirectAttributes) {
        User user = getCurrentUser();
        systemSettingsService.setCurrentSemester(currentSemester.trim());
        systemSettingsService.setAcademicYear(academicYear.trim());
        activityLogService.log("UPDATE_SETTINGS", "SystemSettings", null, user,
                "Updated current semester to " + currentSemester + " and academic year to " + academicYear);
        redirectAttributes.addFlashAttribute("successMessage", "Settings updated successfully.");
        return "redirect:/admin/settings";
    }

    @PostMapping("/period/{id}")
    public String updatePeriod(@PathVariable Long id,
                               @RequestParam String startTime,
                               @RequestParam String endTime,
                               RedirectAttributes redirectAttributes) {
        User user = getCurrentUser();
        try {
            LocalTime start = LocalTime.parse(startTime);
            LocalTime end = LocalTime.parse(endTime);
            systemSettingsService.updatePeriodTiming(id, start, end);
            activityLogService.log("UPDATE_PERIOD", "Period", id, user, "Updated period timing: " + start + " - " + end);
            redirectAttributes.addFlashAttribute("successMessage", "Period timing updated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid time format: " + e.getMessage());
        }
        return "redirect:/admin/settings";
    }
}
