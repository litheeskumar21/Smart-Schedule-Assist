package com.smartschedule.controller.admin;

import com.smartschedule.model.*;
import com.smartschedule.repository.UserRepository;
import com.smartschedule.service.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.DayOfWeek;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/absence-requests")
public class AbsenceManagementController {

    private final AbsenceService absenceService;
    private final SubstituteService substituteService;
    private final ActivityLogService activityLogService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public AbsenceManagementController(AbsenceService absenceService,
                                       SubstituteService substituteService,
                                       ActivityLogService activityLogService,
                                       NotificationService notificationService,
                                       UserRepository userRepository) {
        this.absenceService = absenceService;
        this.substituteService = substituteService;
        this.activityLogService = activityLogService;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByUsername(auth.getName()).orElseThrow();
    }

    @GetMapping
    public String listRequests(Model model) {
        User user = getCurrentUser();
        model.addAttribute("requests", absenceService.getAllAbsenceRequests());
        model.addAttribute("unreadCount", notificationService.getUnreadCount(user));
        return "admin/absence-requests";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        User user = getCurrentUser();
        AbsenceRequest request = absenceService.getAbsenceRequestById(id);
        List<AbsenceAffectedClass> affectedClasses = absenceService.getAffectedClasses(id);

        DayOfWeek day = request.getAbsenceDate().getDayOfWeek();
        Map<Long, List<Teacher>> availableTeachersMap = new HashMap<>();
        for (AbsenceAffectedClass ac : affectedClasses) {
            List<Teacher> available = substituteService.getAvailableSubstitutes(
                    day, ac.getPeriod(), request.getTeacher().getId());
            availableTeachersMap.put(ac.getId(), available);
        }

        model.addAttribute("request", request);
        model.addAttribute("affectedClasses", affectedClasses);
        model.addAttribute("availableTeachersMap", availableTeachersMap);
        model.addAttribute("unreadCount", notificationService.getUnreadCount(user));

        return "admin/absence-detail";
    }

    @PostMapping("/{id}/approve")
    public String approveRequest(@PathVariable Long id,
                                 @RequestParam(required = false, defaultValue = "") String adminComment,
                                 RedirectAttributes redirectAttributes) {
        User admin = getCurrentUser();
        try {
            AbsenceRequest req = absenceService.approveAbsence(id, adminComment, admin);
            activityLogService.log("APPROVE_ABSENCE", "AbsenceRequest", id, admin,
                    "Approved absence for " + req.getTeacher().getUser().getFullName() + " on " + req.getAbsenceDate());
            redirectAttributes.addFlashAttribute("successMessage", "Absence request approved. You can now assign substitute teachers or cancel individual classes below.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error approving absence: " + e.getMessage());
        }
        return "redirect:/admin/absence-requests/" + id;
    }

    @PostMapping("/{id}/reject")
    public String rejectRequest(@PathVariable Long id,
                                @RequestParam(required = false, defaultValue = "") String adminComment,
                                RedirectAttributes redirectAttributes) {
        User admin = getCurrentUser();
        try {
            AbsenceRequest req = absenceService.rejectAbsence(id, adminComment, admin);
            activityLogService.log("REJECT_ABSENCE", "AbsenceRequest", id, admin,
                    "Rejected absence for " + req.getTeacher().getUser().getFullName() + " on " + req.getAbsenceDate());
            redirectAttributes.addFlashAttribute("successMessage", "Absence request rejected.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error rejecting absence: " + e.getMessage());
        }
        return "redirect:/admin/absence-requests";
    }
}
