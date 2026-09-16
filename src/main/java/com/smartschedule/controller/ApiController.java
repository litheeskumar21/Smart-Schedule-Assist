package com.smartschedule.controller;

import com.smartschedule.model.*;
import com.smartschedule.model.enums.UserRole;
import com.smartschedule.repository.PeriodRepository;
import com.smartschedule.repository.UserRepository;
import com.smartschedule.service.StudentService;
import com.smartschedule.service.SubstituteService;
import com.smartschedule.service.TeacherService;
import com.smartschedule.service.TimetableService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final TimetableService timetableService;
    private final SubstituteService substituteService;
    private final UserRepository userRepository;
    private final StudentService studentService;
    private final TeacherService teacherService;
    private final PeriodRepository periodRepository;

    public ApiController(TimetableService timetableService,
                         SubstituteService substituteService,
                         UserRepository userRepository,
                         StudentService studentService,
                         TeacherService teacherService,
                         PeriodRepository periodRepository) {
        this.timetableService = timetableService;
        this.substituteService = substituteService;
        this.userRepository = userRepository;
        this.studentService = studentService;
        this.teacherService = teacherService;
        this.periodRepository = periodRepository;
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByUsername(auth.getName()).orElseThrow();
    }

    @GetMapping("/timetable/next-class")
    public ResponseEntity<?> getNextClass() {
        User user = getCurrentUser();
        TimetableEntry nextClass = null;

        if (user.getRole() == UserRole.ROLE_STUDENT) {
            Student student = studentService.getStudentByUser(user);
            if (student.getSection() != null) {
                nextClass = timetableService.getNextClass(student.getSection());
            }
        } else if (user.getRole() == UserRole.ROLE_TEACHER) {
            Teacher teacher = teacherService.getTeacherByUser(user);
            nextClass = timetableService.getNextClassForTeacher(teacher);
        }

        if (nextClass != null) {
            Map<String, Object> data = new HashMap<>();
            data.put("subject", nextClass.getSubject() != null ? nextClass.getSubject().getName() : "Free");
            data.put("subjectCode", nextClass.getSubject() != null ? nextClass.getSubject().getCode() : "");
            data.put("teacher", nextClass.getTeacher() != null ? nextClass.getTeacher().getUser().getFullName() : "");
            data.put("room", nextClass.getClassroom() != null ? nextClass.getClassroom().getName() : "");
            data.put("startTime", nextClass.getPeriod().getStartTime().toString());
            data.put("endTime", nextClass.getPeriod().getEndTime().toString());
            data.put("periodNumber", nextClass.getPeriod().getPeriodNumber());
            data.put("dayOfWeek", nextClass.getDayOfWeek().name());
            data.put("status", nextClass.getStatus().name());
            return ResponseEntity.ok(Map.of("hasNext", true, "class", data));
        }

        return ResponseEntity.ok(Map.of("hasNext", false));
    }

    @GetMapping("/teachers/available")
    public ResponseEntity<?> getAvailableTeachers(@RequestParam String dayOfWeek, @RequestParam Long periodId) {
        try {
            DayOfWeek day = DayOfWeek.valueOf(dayOfWeek.toUpperCase());
            Period period = periodRepository.findById(periodId).orElseThrow();
            List<Teacher> available = substituteService.getAvailableSubstitutes(day, period, null);

            List<Map<String, Object>> list = available.stream().map(t -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", t.getId());
                m.put("teacherId", t.getTeacherId());
                m.put("name", t.getUser().getFullName());
                return m;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(list);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
