package com.smartschedule.controller.admin;

import com.smartschedule.model.Classroom;
import com.smartschedule.model.User;
import com.smartschedule.model.enums.RoomType;
import com.smartschedule.repository.ClassroomRepository;
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
@RequestMapping("/admin/classrooms")
public class ClassroomController {

    private final ClassroomRepository classroomRepository;
    private final ActivityLogService activityLogService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public ClassroomController(ClassroomRepository classroomRepository,
                               ActivityLogService activityLogService,
                               NotificationService notificationService,
                               UserRepository userRepository) {
        this.classroomRepository = classroomRepository;
        this.activityLogService = activityLogService;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByUsername(auth.getName()).orElseThrow();
    }

    @GetMapping
    public String listClassrooms(Model model) {
        User user = getCurrentUser();
        model.addAttribute("classrooms", classroomRepository.findAll());
        model.addAttribute("roomTypes", RoomType.values());
        model.addAttribute("unreadCount", notificationService.getUnreadCount(user));
        return "admin/classrooms";
    }

    @PostMapping
    public String createClassroom(@RequestParam String name,
                                  @RequestParam(required = false, defaultValue = "CLASSROOM") RoomType roomType,
                                  @RequestParam(defaultValue = "60") int capacity,
                                  RedirectAttributes redirectAttributes) {
        User user = getCurrentUser();
        if (classroomRepository.findByName(name.trim()).isPresent()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Classroom already exists: " + name);
            return "redirect:/admin/classrooms";
        }

        Classroom classroom = new Classroom();
        classroom.setName(name.trim());
        classroom.setRoomType(roomType);
        classroom.setCapacity(capacity > 0 ? capacity : 60);
        classroomRepository.save(classroom);

        activityLogService.log("CREATE_CLASSROOM", "Classroom", classroom.getId(), user, "Created classroom: " + name);
        redirectAttributes.addFlashAttribute("successMessage", "Classroom created successfully.");
        return "redirect:/admin/classrooms";
    }

    @GetMapping("/edit/{id}")
    public String editClassroomForm(@PathVariable Long id, Model model) {
        User user = getCurrentUser();
        model.addAttribute("editClassroom", classroomRepository.findById(id).orElseThrow());
        model.addAttribute("classrooms", classroomRepository.findAll());
        model.addAttribute("roomTypes", RoomType.values());
        model.addAttribute("unreadCount", notificationService.getUnreadCount(user));
        return "admin/classrooms";
    }

    @PostMapping("/edit/{id}")
    public String updateClassroom(@PathVariable Long id,
                                  @RequestParam String name,
                                  @RequestParam(required = false, defaultValue = "CLASSROOM") RoomType roomType,
                                  @RequestParam(defaultValue = "60") int capacity,
                                  RedirectAttributes redirectAttributes) {
        User user = getCurrentUser();
        Classroom classroom = classroomRepository.findById(id).orElseThrow();
        classroom.setName(name.trim());
        classroom.setRoomType(roomType);
        classroom.setCapacity(capacity > 0 ? capacity : 60);
        classroomRepository.save(classroom);

        activityLogService.log("UPDATE_CLASSROOM", "Classroom", classroom.getId(), user, "Updated classroom: " + name);
        redirectAttributes.addFlashAttribute("successMessage", "Classroom updated successfully.");
        return "redirect:/admin/classrooms";
    }

    @PostMapping("/delete/{id}")
    public String deleteClassroom(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        User user = getCurrentUser();
        try {
            Classroom classroom = classroomRepository.findById(id).orElseThrow();
            classroomRepository.delete(classroom);
            activityLogService.log("DELETE_CLASSROOM", "Classroom", id, user, "Deleted classroom: " + classroom.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Classroom deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Cannot delete classroom as it is referenced by timetables.");
        }
        return "redirect:/admin/classrooms";
    }
}
