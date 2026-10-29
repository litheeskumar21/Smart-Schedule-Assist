package com.smartschedule.controller;

import com.smartschedule.dto.AbsenceRequestDTO;
import com.smartschedule.dto.PolicyValidationResult;
import com.smartschedule.model.*;
import com.smartschedule.model.enums.AbsenceType;
import com.smartschedule.model.enums.UserRole;
import com.smartschedule.repository.PeriodRepository;
import com.smartschedule.repository.UserRepository;
import com.smartschedule.service.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
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
    private final OllamaService ollamaService;
    private final AbsencePolicyService absencePolicyService;

    public ApiController(TimetableService timetableService,
                         SubstituteService substituteService,
                         UserRepository userRepository,
                         StudentService studentService,
                         TeacherService teacherService,
                         PeriodRepository periodRepository,
                         OllamaService ollamaService,
                         AbsencePolicyService absencePolicyService) {
        this.timetableService = timetableService;
        this.substituteService = substituteService;
        this.userRepository = userRepository;
        this.studentService = studentService;
        this.teacherService = teacherService;
        this.periodRepository = periodRepository;
        this.ollamaService = ollamaService;
        this.absencePolicyService = absencePolicyService;
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

    @GetMapping("/ai/status")
    public ResponseEntity<?> getAiStatus() {
        boolean available = ollamaService.isOllamaAvailable();
        List<String> models = ollamaService.getAvailableModels();
        return ResponseEntity.ok(Map.of(
                "online", available,
                "models", models,
                "configuredModel", "qwen2.5:3b"
        ));
    }

    @PostMapping("/ai/parse-absence")
    public ResponseEntity<?> parseAbsenceWithAi(@RequestBody Map<String, String> payload) {
        String prompt = payload.get("prompt");
        if (prompt == null || prompt.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Prompt is required"));
        }
        AbsenceRequestDTO parsed = ollamaService.parseAbsencePrompt(prompt);
        return ResponseEntity.ok(parsed);
    }

    @GetMapping("/policy/check")
    public ResponseEntity<?> checkPolicy(@RequestParam String date, @RequestParam(defaultValue = "FULL_DAY") String type) {
        User user = getCurrentUser();
        if (user.getRole() != UserRole.ROLE_TEACHER) {
            return ResponseEntity.ok(Map.of("valid", true));
        }
        try {
            Teacher teacher = teacherService.getTeacherByUser(user);
            LocalDate absenceDate = LocalDate.parse(date);
            AbsenceType absenceType = AbsenceType.valueOf(type.toUpperCase());

            PolicyValidationResult result = absencePolicyService.validateAbsenceRequest(
                    teacher, absenceDate, absenceType, null, null);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
