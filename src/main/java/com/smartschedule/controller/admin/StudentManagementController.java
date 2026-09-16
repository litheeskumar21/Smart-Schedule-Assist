package com.smartschedule.controller.admin;

import com.smartschedule.dto.ImportResult;
import com.smartschedule.dto.StudentDTO;
import com.smartschedule.model.Student;
import com.smartschedule.model.User;
import com.smartschedule.repository.AcademicYearRepository;
import com.smartschedule.repository.DepartmentRepository;
import com.smartschedule.repository.SectionRepository;
import com.smartschedule.repository.UserRepository;
import com.smartschedule.service.ActivityLogService;
import com.smartschedule.service.ImportService;
import com.smartschedule.service.NotificationService;
import com.smartschedule.service.StudentService;
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
@RequestMapping("/admin/students")
public class StudentManagementController {

    private final StudentService studentService;
    private final ImportService importService;
    private final DepartmentRepository departmentRepository;
    private final AcademicYearRepository academicYearRepository;
    private final SectionRepository sectionRepository;
    private final ActivityLogService activityLogService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public StudentManagementController(StudentService studentService,
                                       ImportService importService,
                                       DepartmentRepository departmentRepository,
                                       AcademicYearRepository academicYearRepository,
                                       SectionRepository sectionRepository,
                                       ActivityLogService activityLogService,
                                       NotificationService notificationService,
                                       UserRepository userRepository) {
        this.studentService = studentService;
        this.importService = importService;
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
    public String listStudents(Model model) {
        User user = getCurrentUser();
        model.addAttribute("students", studentService.getAllStudents());
        model.addAttribute("departments", departmentRepository.findAll());
        model.addAttribute("years", academicYearRepository.findAll());
        model.addAttribute("sections", sectionRepository.findAll());
        model.addAttribute("unreadCount", notificationService.getUnreadCount(user));
        return "admin/students";
    }

    @GetMapping("/create")
    public String createStudentForm(Model model) {
        User user = getCurrentUser();
        model.addAttribute("student", new Student());
        model.addAttribute("departments", departmentRepository.findAll());
        model.addAttribute("sections", sectionRepository.findAll());
        model.addAttribute("years", academicYearRepository.findAll());
        model.addAttribute("isEdit", false);
        model.addAttribute("unreadCount", notificationService.getUnreadCount(user));
        return "admin/student-form";
    }

    @PostMapping("/create")
    public String createStudent(@ModelAttribute StudentDTO dto, RedirectAttributes redirectAttributes) {
        User user = getCurrentUser();
        try {
            Student s = studentService.createStudent(dto);
            activityLogService.log("CREATE_STUDENT", "Student", s.getId(), user, "Created student: " + dto.getStudentId());
            redirectAttributes.addFlashAttribute("successMessage", "Student created successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error creating student: " + e.getMessage());
        }
        return "redirect:/admin/students";
    }

    @GetMapping("/edit/{id}")
    public String editStudentForm(@PathVariable Long id, Model model) {
        User user = getCurrentUser();
        Student student = studentService.getStudentById(id);
        model.addAttribute("student", student);
        model.addAttribute("departments", departmentRepository.findAll());
        model.addAttribute("sections", sectionRepository.findAll());
        model.addAttribute("years", academicYearRepository.findAll());
        model.addAttribute("isEdit", true);
        model.addAttribute("unreadCount", notificationService.getUnreadCount(user));
        return "admin/student-form";
    }

    @PostMapping("/edit/{id}")
    public String updateStudent(@PathVariable Long id, @ModelAttribute StudentDTO dto, RedirectAttributes redirectAttributes) {
        User user = getCurrentUser();
        try {
            studentService.updateStudent(id, dto);
            activityLogService.log("UPDATE_STUDENT", "Student", id, user, "Updated student ID: " + dto.getStudentId());
            redirectAttributes.addFlashAttribute("successMessage", "Student updated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating student: " + e.getMessage());
        }
        return "redirect:/admin/students";
    }

    @PostMapping("/delete/{id}")
    public String deleteStudent(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        User user = getCurrentUser();
        try {
            Student s = studentService.getStudentById(id);
            studentService.deleteStudent(id);
            activityLogService.log("DELETE_STUDENT", "Student", id, user, "Deleted student: " + s.getStudentId());
            redirectAttributes.addFlashAttribute("successMessage", "Student deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting student: " + e.getMessage());
        }
        return "redirect:/admin/students";
    }

    @PostMapping("/import")
    public String importStudents(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
        User user = getCurrentUser();
        try {
            ImportResult result = importService.importStudentsFromFile(file);
            redirectAttributes.addFlashAttribute("importResult", result);
            activityLogService.log("IMPORT_STUDENTS", "Student", null, user,
                    "Imported " + result.getSuccessCount() + " students with " + result.getErrorCount() + " errors.");
            if (result.getErrorCount() == 0) {
                redirectAttributes.addFlashAttribute("successMessage", "Successfully imported " + result.getSuccessCount() + " students.");
            } else {
                redirectAttributes.addFlashAttribute("warningMessage", "Import completed with " + result.getErrorCount() + " errors. Valid records imported: " + result.getSuccessCount());
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error importing file: " + e.getMessage());
        }
        return "redirect:/admin/students";
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> downloadTemplate() {
        String csv = "Student ID,Full Name,Email,Password,Department Code,Year Number,Section Name,Semester\n" +
                "S101,John Doe,john@smartschedule.edu,student123,CSE,2,A,3\n" +
                "S102,Jane Smith,jane@smartschedule.edu,student123,CSE,2,B,3\n";
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"students_template.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(bytes);
    }
}
