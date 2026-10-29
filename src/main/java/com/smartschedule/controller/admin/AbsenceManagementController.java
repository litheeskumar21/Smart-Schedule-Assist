package com.smartschedule.controller.admin;

import com.smartschedule.dto.ReplacementPlanDTO;
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
    private final AbsencePolicyService absencePolicyService;

    public AbsenceManagementController(AbsenceService absenceService,
                                       SubstituteService substituteService,
                                       ActivityLogService activityLogService,
                                       NotificationService notificationService,
                                       UserRepository userRepository,
                                       AbsencePolicyService absencePolicyService) {
        this.absenceService = absenceService;
        this.substituteService = substituteService;
        this.activityLogService = activityLogService;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
        this.absencePolicyService = absencePolicyService;
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

        boolean hasProposedReplacements = affectedClasses.stream()
                .anyMatch(ac -> ac.getProposedSubstitute() != null);

        model.addAttribute("request", request);
        model.addAttribute("affectedClasses", affectedClasses);
        model.addAttribute("availableTeachersMap", availableTeachersMap);
        model.addAttribute("hasProposedReplacements", hasProposedReplacements);
        model.addAttribute("weeklyLimit", absencePolicyService.getWeeklyLimit());
        model.addAttribute("monthlyLimit", absencePolicyService.getMonthlyLimit());
        model.addAttribute("unreadCount", notificationService.getUnreadCount(user));

        return "admin/absence-detail";
    }

    @PostMapping("/{id}/calculate-replacements")
    public String calculateReplacements(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        User admin = getCurrentUser();
        try {
            ReplacementPlanDTO plan = absenceService.recalculateReplacementPlan(id);
            activityLogService.log("CALCULATE_REPLACEMENTS", "AbsenceRequest", id, admin,
                    "Calculated substitute proposals for request #" + id + ": " + plan.getProposedCount() + " proposed, " + plan.getUnresolvedCount() + " unresolved");
            redirectAttributes.addFlashAttribute("successMessage",
                    "Replacement plan calculated: " + plan.getProposedCount() + " class(es) mapped to eligible substitutes, "
                            + plan.getUnresolvedCount() + " unresolved.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error calculating replacement plan: " + e.getMessage());
        }
        return "redirect:/admin/absence-requests/" + id;
    }

    @PostMapping("/{id}/apply-replacements")
    public String applyReplacements(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        User admin = getCurrentUser();
        try {
            int applied = absenceService.applyProposedReplacements(id, admin);
            activityLogService.log("APPLY_REPLACEMENTS", "AbsenceRequest", id, admin,
                    "Published and applied " + applied + " substitute assignment(s) for request #" + id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Successfully confirmed and published " + applied + " substitute teacher assignment(s)! Assigned teachers and students have been notified.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error applying replacement plan: " + e.getMessage());
        }
        return "redirect:/admin/absence-requests/" + id;
    }

    @PostMapping("/{id}/approve")
    public String approveRequest(@PathVariable Long id,
                                 @RequestParam(required = false, defaultValue = "") String adminComment,
                                 RedirectAttributes redirectAttributes) {
        User admin = getCurrentUser();
        try {
            AbsenceRequest req = absenceService.approveAbsence(id, adminComment, admin);
            String logMsg = "Approved absence for " + req.getTeacher().getUser().getFullName() + " on " + req.getAbsenceDate();
            if (req.isPolicyViolated()) {
                logMsg += " [ADMIN OVERRIDE for policy violation: " + req.getPolicyViolationReason() + "]";
            }
            activityLogService.log("APPROVE_ABSENCE", "AbsenceRequest", id, admin, logMsg);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Absence request approved" + (req.isPolicyViolated() ? " (with Administrative Policy Override)" : "")
                            + ". You can now review proposed substitutes or assign individual replacements below.");
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
