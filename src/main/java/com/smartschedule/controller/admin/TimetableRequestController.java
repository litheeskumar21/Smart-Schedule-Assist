package com.smartschedule.controller.admin;

import com.smartschedule.model.TimetableChangeRequest;
import com.smartschedule.model.TimetableEntry;
import com.smartschedule.model.User;
import com.smartschedule.model.enums.ChangeType;
import com.smartschedule.model.enums.EntryStatus;
import com.smartschedule.model.enums.NotificationType;
import com.smartschedule.model.enums.RequestStatus;
import com.smartschedule.repository.TimetableChangeRequestRepository;
import com.smartschedule.repository.TimetableEntryRepository;
import com.smartschedule.repository.UserRepository;
import com.smartschedule.service.ActivityLogService;
import com.smartschedule.service.NotificationService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/admin/timetable-requests")
public class TimetableRequestController {

    private final TimetableChangeRequestRepository timetableChangeRequestRepository;
    private final TimetableEntryRepository timetableEntryRepository;
    private final ActivityLogService activityLogService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public TimetableRequestController(TimetableChangeRequestRepository timetableChangeRequestRepository,
                                      TimetableEntryRepository timetableEntryRepository,
                                      ActivityLogService activityLogService,
                                      NotificationService notificationService,
                                      UserRepository userRepository) {
        this.timetableChangeRequestRepository = timetableChangeRequestRepository;
        this.timetableEntryRepository = timetableEntryRepository;
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
        model.addAttribute("requests", timetableChangeRequestRepository.findAllByOrderByCreatedAtDesc());
        model.addAttribute("unreadCount", notificationService.getUnreadCount(user));
        return "admin/timetable-requests";
    }

    @PostMapping("/{id}/approve")
    public String approveRequest(@PathVariable Long id,
                                 @RequestParam(required = false, defaultValue = "") String adminComment,
                                 RedirectAttributes redirectAttributes) {
        User admin = getCurrentUser();
        TimetableChangeRequest request = timetableChangeRequestRepository.findById(id).orElse(null);
        if (request == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Request not found.");
            return "redirect:/admin/timetable-requests";
        }

        request.setStatus(RequestStatus.APPROVED);
        request.setAdminComment(adminComment);
        request.setReviewedAt(LocalDateTime.now());
        timetableChangeRequestRepository.save(request);

        TimetableEntry entry = request.getTimetableEntry();
        if (entry != null) {
            if (request.getChangeType() == ChangeType.CANCEL) {
                entry.setStatus(EntryStatus.CANCELLED);
                timetableEntryRepository.save(entry);

                // Notify students in section
                String subName = entry.getSubject() != null ? entry.getSubject().getName() : "Class";
                notificationService.notifyStudentsInSection(entry.getSection(),
                        "Class Cancelled: " + subName,
                        subName + " on " + entry.getDayOfWeek() + " (Period " + entry.getPeriod().getPeriodNumber() + ") has been CANCELLED.",
                        NotificationType.CLASS_CANCELLATION);
            }
        }

        // Notify teacher
        notificationService.notifyTeacher(request.getTeacher(),
                "Timetable Request Approved",
                "Your " + request.getChangeType() + " request for " +
                        (entry != null && entry.getSubject() != null ? entry.getSubject().getName() : "class") +
                        " has been approved.",
                NotificationType.TIMETABLE_CHANGE);

        activityLogService.log("APPROVE_TIMETABLE_REQUEST", "TimetableChangeRequest", id, admin,
                "Approved " + request.getChangeType() + " request from " + request.getTeacher().getUser().getFullName());

        redirectAttributes.addFlashAttribute("successMessage", "Timetable request approved successfully.");
        return "redirect:/admin/timetable-requests";
    }

    @PostMapping("/{id}/reject")
    public String rejectRequest(@PathVariable Long id,
                                @RequestParam(required = false, defaultValue = "") String adminComment,
                                RedirectAttributes redirectAttributes) {
        User admin = getCurrentUser();
        TimetableChangeRequest request = timetableChangeRequestRepository.findById(id).orElse(null);
        if (request == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Request not found.");
            return "redirect:/admin/timetable-requests";
        }

        request.setStatus(RequestStatus.REJECTED);
        request.setAdminComment(adminComment);
        request.setReviewedAt(LocalDateTime.now());
        timetableChangeRequestRepository.save(request);

        String msg = "Your timetable request has been rejected.";
        if (!adminComment.trim().isEmpty()) {
            msg += " Reason: " + adminComment;
        }
        notificationService.notifyTeacher(request.getTeacher(), "Timetable Request Rejected", msg, NotificationType.TIMETABLE_CHANGE);

        activityLogService.log("REJECT_TIMETABLE_REQUEST", "TimetableChangeRequest", id, admin,
                "Rejected timetable request from " + request.getTeacher().getUser().getFullName());

        redirectAttributes.addFlashAttribute("successMessage", "Timetable request rejected.");
        return "redirect:/admin/timetable-requests";
    }
}
