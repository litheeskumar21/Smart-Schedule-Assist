package com.smartschedule.controller.admin;

import com.smartschedule.model.AcademicYear;
import com.smartschedule.model.Department;
import com.smartschedule.model.Section;
import com.smartschedule.model.User;
import com.smartschedule.repository.AcademicYearRepository;
import com.smartschedule.repository.DepartmentRepository;
import com.smartschedule.repository.SectionRepository;
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
@RequestMapping("/admin/sections")
public class SectionController {

    private final SectionRepository sectionRepository;
    private final DepartmentRepository departmentRepository;
    private final AcademicYearRepository academicYearRepository;
    private final ActivityLogService activityLogService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public SectionController(SectionRepository sectionRepository,
                             DepartmentRepository departmentRepository,
                             AcademicYearRepository academicYearRepository,
                             ActivityLogService activityLogService,
                             NotificationService notificationService,
                             UserRepository userRepository) {
        this.sectionRepository = sectionRepository;
        this.departmentRepository = departmentRepository;
        this.academicYearRepository = academicYearRepository;
        this.activityLogService = activityLogService;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByUsername(auth.getName()).orElseThrow();
    }

    @GetMapping
    public String listSections(Model model) {
        User user = getCurrentUser();
        model.addAttribute("sections", sectionRepository.findAll());
        model.addAttribute("departments", departmentRepository.findAll());
        model.addAttribute("years", academicYearRepository.findAll());
        model.addAttribute("unreadCount", notificationService.getUnreadCount(user));
        return "admin/sections";
    }

    @PostMapping
    public String createSection(@RequestParam String name,
                                @RequestParam Long departmentId,
                                @RequestParam Long academicYearId,
                                RedirectAttributes redirectAttributes) {
        User user = getCurrentUser();
        Department dept = departmentRepository.findById(departmentId).orElseThrow();
        AcademicYear yr = academicYearRepository.findById(academicYearId).orElseThrow();

        if (sectionRepository.findByDepartmentAndAcademicYearAndName(dept, yr, name.trim()).isPresent()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Section " + name + " already exists for this department and year.");
            return "redirect:/admin/sections";
        }

        Section section = new Section();
        section.setName(name.trim().toUpperCase());
        section.setDepartment(dept);
        section.setAcademicYear(yr);
        section.setDisplayName(dept.getCode() + "-" + name.trim().toUpperCase());
        sectionRepository.save(section);

        activityLogService.log("CREATE_SECTION", "Section", section.getId(), user, "Created section: " + section.getDisplayName());
        redirectAttributes.addFlashAttribute("successMessage", "Section created successfully.");
        return "redirect:/admin/sections";
    }

    @GetMapping("/edit/{id}")
    public String editSectionForm(@PathVariable Long id, Model model) {
        User user = getCurrentUser();
        model.addAttribute("editSection", sectionRepository.findById(id).orElseThrow());
        model.addAttribute("sections", sectionRepository.findAll());
        model.addAttribute("departments", departmentRepository.findAll());
        model.addAttribute("years", academicYearRepository.findAll());
        model.addAttribute("unreadCount", notificationService.getUnreadCount(user));
        return "admin/sections";
    }

    @PostMapping("/edit/{id}")
    public String updateSection(@PathVariable Long id,
                                @RequestParam String name,
                                @RequestParam Long departmentId,
                                @RequestParam Long academicYearId,
                                RedirectAttributes redirectAttributes) {
        User user = getCurrentUser();
        Section section = sectionRepository.findById(id).orElseThrow();
        Department dept = departmentRepository.findById(departmentId).orElseThrow();
        AcademicYear yr = academicYearRepository.findById(academicYearId).orElseThrow();

        section.setName(name.trim().toUpperCase());
        section.setDepartment(dept);
        section.setAcademicYear(yr);
        section.setDisplayName(dept.getCode() + "-" + name.trim().toUpperCase());
        sectionRepository.save(section);

        activityLogService.log("UPDATE_SECTION", "Section", section.getId(), user, "Updated section: " + section.getDisplayName());
        redirectAttributes.addFlashAttribute("successMessage", "Section updated successfully.");
        return "redirect:/admin/sections";
    }

    @PostMapping("/delete/{id}")
    public String deleteSection(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        User user = getCurrentUser();
        try {
            Section section = sectionRepository.findById(id).orElseThrow();
            sectionRepository.delete(section);
            activityLogService.log("DELETE_SECTION", "Section", id, user, "Deleted section: " + section.getDisplayName());
            redirectAttributes.addFlashAttribute("successMessage", "Section deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Cannot delete section as it is referenced by students, subjects, or timetables.");
        }
        return "redirect:/admin/sections";
    }
}
