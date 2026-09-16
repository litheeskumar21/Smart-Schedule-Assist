package com.smartschedule.controller.admin;

import com.smartschedule.dto.ImportResult;
import com.smartschedule.dto.TeacherDTO;
import com.smartschedule.model.Teacher;
import com.smartschedule.model.User;
import com.smartschedule.repository.DepartmentRepository;
import com.smartschedule.repository.UserRepository;
import com.smartschedule.service.ActivityLogService;
import com.smartschedule.service.ImportService;
import com.smartschedule.service.NotificationService;
import com.smartschedule.service.TeacherService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/admin/teachers")
public class TeacherManagementController {

    private final TeacherService teacherService;
    private final ImportService importService;
    private final DepartmentRepository departmentRepository;
    private final ActivityLogService activityLogService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public TeacherManagementController(TeacherService teacherService,
                                       ImportService importService,
                                       DepartmentRepository departmentRepository,
                                       ActivityLogService activityLogService,
                                       NotificationService notificationService,
                                       UserRepository userRepository) {
        this.teacherService = teacherService;
        this.importService = importService;
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
    public String listTeachers(Model model) {
        User user = getCurrentUser();
        model.addAttribute("teachers", teacherService.getAllTeachers());
        model.addAttribute("departments", departmentRepository.findAll());
        model.addAttribute("unreadCount", notificationService.getUnreadCount(user));
        return "admin/teachers";
    }

    @GetMapping("/create")
    public String createTeacherForm(Model model) {
        User user = getCurrentUser();
        model.addAttribute("teacher", new Teacher());
        model.addAttribute("departments", departmentRepository.findAll());
        model.addAttribute("isEdit", false);
        model.addAttribute("unreadCount", notificationService.getUnreadCount(user));
        return "admin/teacher-form";
    }

    @PostMapping("/create")
    public String createTeacher(@ModelAttribute TeacherDTO dto, RedirectAttributes redirectAttributes) {
        User user = getCurrentUser();
        try {
            Teacher t = teacherService.createTeacher(dto);
            activityLogService.log("CREATE_TEACHER", "Teacher", t.getId(), user, "Created teacher: " + dto.getTeacherId());
            redirectAttributes.addFlashAttribute("successMessage", "Teacher created successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error creating teacher: " + e.getMessage());
        }
        return "redirect:/admin/teachers";
    }

    @GetMapping("/edit/{id}")
    public String editTeacherForm(@PathVariable Long id, Model model) {
        User user = getCurrentUser();
        Teacher teacher = teacherService.getTeacherById(id);
        model.addAttribute("teacher", teacher);
        model.addAttribute("departments", departmentRepository.findAll());
        model.addAttribute("isEdit", true);
        model.addAttribute("unreadCount", notificationService.getUnreadCount(user));
        return "admin/teacher-form";
    }

    @PostMapping("/edit/{id}")
    public String updateTeacher(@PathVariable Long id, @ModelAttribute TeacherDTO dto, RedirectAttributes redirectAttributes) {
        User user = getCurrentUser();
        try {
            teacherService.updateTeacher(id, dto);
            activityLogService.log("UPDATE_TEACHER", "Teacher", id, user, "Updated teacher ID: " + dto.getTeacherId());
            redirectAttributes.addFlashAttribute("successMessage", "Teacher updated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating teacher: " + e.getMessage());
        }
        return "redirect:/admin/teachers";
    }

    @PostMapping("/delete/{id}")
    public String deleteTeacher(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        User user = getCurrentUser();
        try {
            Teacher t = teacherService.getTeacherById(id);
            teacherService.deleteTeacher(id);
            activityLogService.log("DELETE_TEACHER", "Teacher", id, user, "Deleted teacher: " + t.getTeacherId());
            redirectAttributes.addFlashAttribute("successMessage", "Teacher deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting teacher: " + e.getMessage());
        }
        return "redirect:/admin/teachers";
    }

    @PostMapping("/import")
    public String importTeachers(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
        User user = getCurrentUser();
        try {
            ImportResult result = importService.importTeachersFromFile(file);
            redirectAttributes.addFlashAttribute("importResult", result);
            activityLogService.log("IMPORT_TEACHERS", "Teacher", null, user,
                    "Imported " + result.getSuccessCount() + " teachers with " + result.getErrorCount() + " errors.");
            if (result.getErrorCount() == 0) {
                redirectAttributes.addFlashAttribute("successMessage", "Successfully imported " + result.getSuccessCount() + " teachers.");
            } else {
                redirectAttributes.addFlashAttribute("warningMessage", "Import completed with " + result.getErrorCount() + " errors. Valid records imported: " + result.getSuccessCount());
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error importing file: " + e.getMessage());
        }
        return "redirect:/admin/teachers";
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> downloadTemplate() {
        String csv = "Teacher ID,Full Name,Email,Password,Phone,Department Codes\n" +
                "T101,Dr. Alan Turing,alan@smartschedule.edu,teacher123,9876543210,\"CSE,ECE\"\n" +
                "T102,Prof. Grace Hopper,grace@smartschedule.edu,teacher123,9876543211,CSE\n";
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"teachers_template.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(bytes);
    }
}
