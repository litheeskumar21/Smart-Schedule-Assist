package com.smartschedule.controller.admin;

import com.smartschedule.model.Department;
import com.smartschedule.model.User;
import com.smartschedule.repository.DepartmentRepository;
import com.smartschedule.repository.UserRepository;
import com.smartschedule.service.ActivityLogService;
import com.smartschedule.service.NotificationService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/departments")
public class DepartmentController {

    private final DepartmentRepository departmentRepository;
    private final ActivityLogService activityLogService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public DepartmentController(DepartmentRepository departmentRepository,
                                ActivityLogService activityLogService,
                                NotificationService notificationService,
                                UserRepository userRepository) {
        this.departmentRepository = departmentRepository;
        this.activityLogService = activityLogService;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByUsername(auth.getName()).orElseThrow();
    }

    @GetMapping
    public String listDepartments(Model model) {
        User user = getCurrentUser();
        model.addAttribute("departments", departmentRepository.findAll());
        model.addAttribute("unreadCount", notificationService.getUnreadCount(user));
        return "admin/departments";
    }

    @PostMapping
    public String createDepartment(@RequestParam String name, @RequestParam String code, RedirectAttributes redirectAttributes) {
        User user = getCurrentUser();
        if (departmentRepository.findByCode(code.trim().toUpperCase()).isPresent()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Department code already exists: " + code);
            return "redirect:/admin/departments";
        }

        Department dept = new Department();
        dept.setName(name.trim());
        dept.setCode(code.trim().toUpperCase());
        departmentRepository.save(dept);

        activityLogService.log("CREATE_DEPARTMENT", "Department", dept.getId(), user, "Created department: " + name);
        redirectAttributes.addFlashAttribute("successMessage", "Department created successfully.");
        return "redirect:/admin/departments";
    }

    @GetMapping("/edit/{id}")
    public String editDepartmentForm(@PathVariable Long id, Model model) {
        User user = getCurrentUser();
        model.addAttribute("editDepartment", departmentRepository.findById(id).orElseThrow());
        model.addAttribute("departments", departmentRepository.findAll());
        model.addAttribute("unreadCount", notificationService.getUnreadCount(user));
        return "admin/departments";
    }

    @PostMapping("/edit/{id}")
    public String updateDepartment(@PathVariable Long id, @RequestParam String name, @RequestParam String code, RedirectAttributes redirectAttributes) {
        User user = getCurrentUser();
        Department dept = departmentRepository.findById(id).orElseThrow();
        dept.setName(name.trim());
        dept.setCode(code.trim().toUpperCase());
        departmentRepository.save(dept);

        activityLogService.log("UPDATE_DEPARTMENT", "Department", dept.getId(), user, "Updated department: " + name);
        redirectAttributes.addFlashAttribute("successMessage", "Department updated successfully.");
        return "redirect:/admin/departments";
    }

    @PostMapping("/delete/{id}")
    public String deleteDepartment(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        User user = getCurrentUser();
        try {
            Department dept = departmentRepository.findById(id).orElseThrow();
            departmentRepository.delete(dept);
            activityLogService.log("DELETE_DEPARTMENT", "Department", id, user, "Deleted department: " + dept.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Department deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Cannot delete department as it is referenced by sections, students, or teachers.");
        }
        return "redirect:/admin/departments";
    }
}
