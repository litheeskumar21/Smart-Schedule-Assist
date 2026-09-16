package com.smartschedule.controller.admin;

import com.smartschedule.dto.ImportResult;
import com.smartschedule.model.User;
import com.smartschedule.repository.UserRepository;
import com.smartschedule.service.ActivityLogService;
import com.smartschedule.service.ImportService;
import com.smartschedule.service.NotificationService;
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
@RequestMapping("/admin/import")
public class ImportController {

    private final ImportService importService;
    private final ActivityLogService activityLogService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public ImportController(ImportService importService,
                            ActivityLogService activityLogService,
                            NotificationService notificationService,
                            UserRepository userRepository) {
        this.importService = importService;
        this.activityLogService = activityLogService;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByUsername(auth.getName()).orElseThrow();
    }

    @GetMapping("/{type}")
    public String importPage(@PathVariable String type, Model model) {
        User user = getCurrentUser();
        model.addAttribute("type", type);
        model.addAttribute("unreadCount", notificationService.getUnreadCount(user));
        return "admin/import";
    }

    @PostMapping("/{type}")
    public String importFile(@PathVariable String type,
                             @RequestParam("file") MultipartFile file,
                             RedirectAttributes redirectAttributes,
                             Model model) {
        User user = getCurrentUser();
        try {
            ImportResult result;
            switch (type.toLowerCase()) {
                case "students":
                    result = importService.importStudentsFromFile(file);
                    break;
                case "teachers":
                    result = importService.importTeachersFromFile(file);
                    break;
                case "subjects":
                    result = importService.importSubjectsFromFile(file);
                    break;
                case "classrooms":
                    result = importService.importClassroomsFromFile(file);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown import category: " + type);
            }

            model.addAttribute("importResult", result);
            model.addAttribute("type", type);
            model.addAttribute("unreadCount", notificationService.getUnreadCount(user));

            activityLogService.log("IMPORT_" + type.toUpperCase(), type, null, user,
                    "Imported " + result.getSuccessCount() + " records with " + result.getErrorCount() + " errors.");

            return "admin/import";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error during import: " + e.getMessage());
            return "redirect:/admin/import/" + type;
        }
    }

    @GetMapping("/template/{type}")
    public ResponseEntity<byte[]> getTemplate(@PathVariable String type) {
        String csvContent;
        String filename;

        switch (type.toLowerCase()) {
            case "students":
                csvContent = "Student ID,Full Name,Email,Password,Department Code,Year Number,Section Name,Semester\n" +
                        "S101,John Doe,john@smartschedule.edu,student123,CSE,2,A,3\n" +
                        "S102,Jane Smith,jane@smartschedule.edu,student123,CSE,2,B,3\n";
                filename = "students_template.csv";
                break;
            case "teachers":
                csvContent = "Teacher ID,Full Name,Email,Password,Phone,Department Codes\n" +
                        "T101,Dr. Alan Turing,alan@smartschedule.edu,teacher123,9876543210,\"CSE,ECE\"\n" +
                        "T102,Prof. Grace Hopper,grace@smartschedule.edu,teacher123,9876543211,CSE\n";
                filename = "teachers_template.csv";
                break;
            case "subjects":
                csvContent = "Subject Code,Subject Name,Type,Periods Per Week,Department Code,Year Number\n" +
                        "CS301,Artificial Intelligence,THEORY,4,CSE,3\n" +
                        "CS302,AI Lab,LAB,2,CSE,3\n";
                filename = "subjects_template.csv";
                break;
            case "classrooms":
                csvContent = "Room Name,Room Type,Capacity\n" +
                        "Room 301,CLASSROOM,60\n" +
                        "AI Lab,COMPUTER_LAB,30\n";
                filename = "classrooms_template.csv";
                break;
            default:
                throw new IllegalArgumentException("Unknown template type: " + type);
        }

        byte[] bytes = csvContent.getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(bytes);
    }
}
