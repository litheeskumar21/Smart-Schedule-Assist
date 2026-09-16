package com.smartschedule.controller.admin;

import com.smartschedule.model.AbsenceAffectedClass;
import com.smartschedule.model.User;
import com.smartschedule.repository.AbsenceAffectedClassRepository;
import com.smartschedule.repository.UserRepository;
import com.smartschedule.service.ActivityLogService;
import com.smartschedule.service.SubstituteService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/substitutes")
public class SubstituteController {

    private final SubstituteService substituteService;
    private final AbsenceAffectedClassRepository absenceAffectedClassRepository;
    private final ActivityLogService activityLogService;
    private final UserRepository userRepository;

    public SubstituteController(SubstituteService substituteService,
                                AbsenceAffectedClassRepository absenceAffectedClassRepository,
                                ActivityLogService activityLogService,
                                UserRepository userRepository) {
        this.substituteService = substituteService;
        this.absenceAffectedClassRepository = absenceAffectedClassRepository;
        this.activityLogService = activityLogService;
        this.userRepository = userRepository;
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByUsername(auth.getName()).orElseThrow();
    }

    @PostMapping("/assign")
    public String assignSubstitute(@RequestParam Long affectedClassId,
                                   @RequestParam Long substituteTeacherId,
                                   RedirectAttributes redirectAttributes) {
        User admin = getCurrentUser();
        Long requestId = null;
        try {
            AbsenceAffectedClass ac = absenceAffectedClassRepository.findById(affectedClassId).orElse(null);
            if (ac != null) requestId = ac.getAbsenceRequest().getId();

            substituteService.assignSubstitute(affectedClassId, substituteTeacherId, admin);
            activityLogService.log("ASSIGN_SUBSTITUTE", "AbsenceAffectedClass", affectedClassId, admin,
                    "Assigned substitute teacher ID " + substituteTeacherId + " for affected class ID " + affectedClassId);
            redirectAttributes.addFlashAttribute("successMessage", "Substitute teacher assigned successfully. Timetable updated and notifications sent.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error assigning substitute: " + e.getMessage());
        }

        if (requestId != null) {
            return "redirect:/admin/absence-requests/" + requestId;
        }
        return "redirect:/admin/absence-requests";
    }

    @PostMapping("/cancel-class")
    public String cancelClass(@RequestParam Long affectedClassId, RedirectAttributes redirectAttributes) {
        User admin = getCurrentUser();
        Long requestId = null;
        try {
            AbsenceAffectedClass ac = absenceAffectedClassRepository.findById(affectedClassId).orElse(null);
            if (ac != null) requestId = ac.getAbsenceRequest().getId();

            substituteService.cancelClass(affectedClassId, admin);
            activityLogService.log("CANCEL_CLASS", "AbsenceAffectedClass", affectedClassId, admin,
                    "Cancelled affected class ID " + affectedClassId);
            redirectAttributes.addFlashAttribute("successMessage", "Class has been marked as CANCELLED. Timetable updated and students notified.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error cancelling class: " + e.getMessage());
        }

        if (requestId != null) {
            return "redirect:/admin/absence-requests/" + requestId;
        }
        return "redirect:/admin/absence-requests";
    }
}
