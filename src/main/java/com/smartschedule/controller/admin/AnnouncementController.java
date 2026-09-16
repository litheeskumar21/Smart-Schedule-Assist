package com.smartschedule.controller.admin;

import com.smartschedule.dto.AnnouncementDTO;
import com.smartschedule.model.Announcement;
import com.smartschedule.model.User;
import com.smartschedule.model.enums.TargetType;
import com.smartschedule.repository.AcademicYearRepository;
import com.smartschedule.repository.DepartmentRepository;
import com.smartschedule.repository.SectionRepository;
import com.smartschedule.repository.UserRepository;
import com.smartschedule.service.ActivityLogService;
import com.smartschedule.service.AnnouncementService;
import com.smartschedule.service.NotificationService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/announcements")
public class AnnouncementController {

    private final AnnouncementService announcementService;
    private final DepartmentRepository departmentRepository;
    private final AcademicYearRepository academicYearRepository;
    private final SectionRepository sectionRepository;
    private final ActivityLogService activityLogService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public AnnouncementController(AnnouncementService announcementService,
                                  DepartmentRepository departmentRepository,
                                  AcademicYearRepository academicYearRepository,
                                  SectionRepository sectionRepository,
                                  ActivityLogService activityLogService,
                                  NotificationService notificationService,
                                  UserRepository userRepository) {
        this.announcementService = announcementService;
        this.departmentRepository = departmentRepository;
        this.academicYearRepository = academicYearRepository;
        this.sectionRepository = sectionRepository;
        this.activityLogService = activityLogService;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByUsername(auth.getName()).orElseThrow();
    }

    @GetMapping
    public String listAnnouncements(Model model) {
        User user = getCurrentUser();
        model.addAttribute("announcements", announcementService.getAllAnnouncements());
        model.addAttribute("unreadCount", notificationService.getUnreadCount(user));
        return "admin/announcements";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        User user = getCurrentUser();
        model.addAttribute("announcement", new Announcement());
        model.addAttribute("targetTypes", TargetType.values());
        model.addAttribute("departments", departmentRepository.findAll());
        model.addAttribute("years", academicYearRepository.findAll());
        model.addAttribute("sections", sectionRepository.findAll());
        model.addAttribute("isEdit", false);
        model.addAttribute("unreadCount", notificationService.getUnreadCount(user));
        return "admin/announcement-form";
    }

    @PostMapping("/create")
    public String create(@ModelAttribute AnnouncementDTO dto, RedirectAttributes redirectAttributes) {
        User user = getCurrentUser();
        try {
            Announcement a = announcementService.createAnnouncement(dto, user);
            activityLogService.log("CREATE_ANNOUNCEMENT", "Announcement", a.getId(), user, "Created announcement: " + dto.getTitle());
            redirectAttributes.addFlashAttribute("successMessage", "Announcement published successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error creating announcement: " + e.getMessage());
        }
        return "redirect:/admin/announcements";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        User user = getCurrentUser();
        Announcement announcement = announcementService.getAnnouncementById(id);
        model.addAttribute("announcement", announcement);
        model.addAttribute("targetTypes", TargetType.values());
        model.addAttribute("departments", departmentRepository.findAll());
        model.addAttribute("years", academicYearRepository.findAll());
        model.addAttribute("sections", sectionRepository.findAll());
        model.addAttribute("isEdit", true);
        model.addAttribute("unreadCount", notificationService.getUnreadCount(user));
        return "admin/announcement-form";
    }

    @PostMapping("/edit/{id}")
    public String update(@PathVariable Long id, @ModelAttribute AnnouncementDTO dto, RedirectAttributes redirectAttributes) {
        User user = getCurrentUser();
        try {
            announcementService.updateAnnouncement(id, dto);
            activityLogService.log("UPDATE_ANNOUNCEMENT", "Announcement", id, user, "Updated announcement: " + dto.getTitle());
            redirectAttributes.addFlashAttribute("successMessage", "Announcement updated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating announcement: " + e.getMessage());
        }
        return "redirect:/admin/announcements";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        User user = getCurrentUser();
        try {
            announcementService.deleteAnnouncement(id);
            activityLogService.log("DELETE_ANNOUNCEMENT", "Announcement", id, user, "Deleted announcement ID: " + id);
            redirectAttributes.addFlashAttribute("successMessage", "Announcement deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting announcement: " + e.getMessage());
        }
        return "redirect:/admin/announcements";
    }
}
