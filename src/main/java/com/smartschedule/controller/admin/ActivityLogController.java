package com.smartschedule.controller.admin;

import com.smartschedule.model.ActivityLog;
import com.smartschedule.model.User;
import com.smartschedule.repository.UserRepository;
import com.smartschedule.service.ActivityLogService;
import com.smartschedule.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/activity-log")
public class ActivityLogController {

    private final ActivityLogService activityLogService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public ActivityLogController(ActivityLogService activityLogService,
                                 NotificationService notificationService,
                                 UserRepository userRepository) {
        this.activityLogService = activityLogService;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByUsername(auth.getName()).orElseThrow();
    }

    @GetMapping
    public String viewLogs(@RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "20") int size,
                           Model model) {
        User user = getCurrentUser();
        Page<ActivityLog> logsPage = activityLogService.getActivityLogs(PageRequest.of(page, size));

        model.addAttribute("activityLogs", logsPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", logsPage.getTotalPages());
        model.addAttribute("totalElements", logsPage.getTotalElements());
        model.addAttribute("unreadCount", notificationService.getUnreadCount(user));

        return "admin/activity-log";
    }
}
