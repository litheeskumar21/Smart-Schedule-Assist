package com.smartschedule.controller.admin;

import com.smartschedule.dto.GenerationResult;
import com.smartschedule.dto.TimetableEntryDTO;
import com.smartschedule.model.*;
import com.smartschedule.model.enums.EntryStatus;
import com.smartschedule.model.enums.NotificationType;
import com.smartschedule.repository.*;
import com.smartschedule.service.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/timetables")
public class TimetableController {

    private final TimetableService timetableService;
    private final TimetableGeneratorService timetableGeneratorService;
    private final TimetableValidationService timetableValidationService;
    private final DepartmentRepository departmentRepository;
    private final AcademicYearRepository academicYearRepository;
    private final SectionRepository sectionRepository;
    private final PeriodRepository periodRepository;
    private final SubjectRepository subjectRepository;
    private final TeacherRepository teacherRepository;
    private final ClassroomRepository classroomRepository;
    private final SubjectAssignmentRepository subjectAssignmentRepository;
    private final TimetableEntryRepository timetableEntryRepository;
    private final ActivityLogService activityLogService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public TimetableController(TimetableService timetableService,
                               TimetableGeneratorService timetableGeneratorService,
                               TimetableValidationService timetableValidationService,
                               DepartmentRepository departmentRepository,
                               AcademicYearRepository academicYearRepository,
                               SectionRepository sectionRepository,
                               PeriodRepository periodRepository,
                               SubjectRepository subjectRepository,
                               TeacherRepository teacherRepository,
                               ClassroomRepository classroomRepository,
                               SubjectAssignmentRepository subjectAssignmentRepository,
                               TimetableEntryRepository timetableEntryRepository,
                               ActivityLogService activityLogService,
                               NotificationService notificationService,
                               UserRepository userRepository) {
        this.timetableService = timetableService;
        this.timetableGeneratorService = timetableGeneratorService;
        this.timetableValidationService = timetableValidationService;
        this.departmentRepository = departmentRepository;
        this.academicYearRepository = academicYearRepository;
        this.sectionRepository = sectionRepository;
        this.periodRepository = periodRepository;
        this.subjectRepository = subjectRepository;
        this.teacherRepository = teacherRepository;
        this.classroomRepository = classroomRepository;
        this.subjectAssignmentRepository = subjectAssignmentRepository;
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
    public String listTimetables(@RequestParam(required = false) Long departmentId,
                                 @RequestParam(required = false) Long yearId,
                                 @RequestParam(required = false) Long sectionId,
                                 Model model) {
        User user = getCurrentUser();
        model.addAttribute("departments", departmentRepository.findAll());
        model.addAttribute("years", academicYearRepository.findAll());
        model.addAttribute("sections", sectionRepository.findAll());
        model.addAttribute("teachers", teacherRepository.findAll());
        model.addAttribute("unreadCount", notificationService.getUnreadCount(user));

        if (sectionId != null) {
            Section section = sectionRepository.findById(sectionId).orElse(null);
            if (section != null) {
                model.addAttribute("selectedSection", section);
                model.addAttribute("entries", timetableService.getAllEntriesForSection(section));
                model.addAttribute("weeklyTimetable", timetableService.getWeeklyTimetable(section));
                model.addAttribute("allPeriods", timetableService.getAllPeriods());
            }
        }

        return "admin/timetables";
    }

    @GetMapping("/edit/{sectionId}")
    public String editTimetableForm(@PathVariable Long sectionId, Model model) {
        User user = getCurrentUser();
        Section section = sectionRepository.findById(sectionId).orElseThrow();

        model.addAttribute("section", section);
        model.addAttribute("entries", timetableService.getAllEntriesForSection(section));
        model.addAttribute("allPeriods", timetableService.getAllPeriods());
        model.addAttribute("subjects", subjectRepository.findAll());
        model.addAttribute("teachers", teacherRepository.findAll());
        model.addAttribute("classrooms", classroomRepository.findAll());
        model.addAttribute("subjectAssignments", subjectAssignmentRepository.findBySection(section));
        model.addAttribute("showEditor", true);
        model.addAttribute("unreadCount", notificationService.getUnreadCount(user));

        return "admin/timetable-edit";
    }

    @PostMapping("/generate")
    public String generateTimetable(@RequestParam Long sectionId, RedirectAttributes redirectAttributes) {
        User user = getCurrentUser();
        Section section = sectionRepository.findById(sectionId).orElseThrow();

        List<String> errors = timetableValidationService.validatePreGeneration(section);
        if (!errors.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Cannot generate: " + String.join(" | ", errors));
            return "redirect:/admin/timetables?sectionId=" + sectionId;
        }

        GenerationResult result = timetableGeneratorService.generateAndValidate(section);
        redirectAttributes.addFlashAttribute("generationResult", result);
        redirectAttributes.addFlashAttribute("successMessage", "Timetable generated successfully as draft. Review and publish when ready.");

        activityLogService.log("GENERATE_TIMETABLE", "Section", sectionId, user,
                "Generated draft timetable for " + section.getDisplayName());

        return "redirect:/admin/timetables/edit/" + sectionId;
    }

    @PostMapping("/publish/{sectionId}")
    public String publishTimetable(@PathVariable Long sectionId, RedirectAttributes redirectAttributes) {
        User user = getCurrentUser();
        Section section = sectionRepository.findById(sectionId).orElseThrow();

        timetableService.publishTimetable(sectionId);

        // Notify students in section
        notificationService.notifyStudentsInSection(section,
                "Official Timetable Published",
                "The timetable for " + section.getDisplayName() + " has been published by the administration.",
                NotificationType.TIMETABLE_CHANGE);

        // Notify assigned teachers
        List<SubjectAssignment> assignments = subjectAssignmentRepository.findBySection(section);
        for (SubjectAssignment sa : assignments) {
            if (sa.getTeacher() != null) {
                notificationService.notifyTeacher(sa.getTeacher(),
                        "Timetable Published (" + section.getDisplayName() + ")",
                        "The timetable for " + section.getDisplayName() + " is now published.",
                        NotificationType.TIMETABLE_CHANGE);
            }
        }

        activityLogService.log("PUBLISH_TIMETABLE", "Section", sectionId, user,
                "Published timetable for " + section.getDisplayName());

        redirectAttributes.addFlashAttribute("successMessage", "Timetable published successfully! Affected students and teachers have been notified.");
        return "redirect:/admin/timetables?sectionId=" + sectionId;
    }

    @PostMapping("/lock")
    @ResponseBody
    public ResponseEntity<?> toggleLock(@RequestParam Long entryId) {
        TimetableEntry entry = timetableEntryRepository.findById(entryId).orElse(null);
        if (entry != null) {
            entry.setLocked(!entry.isLocked());
            timetableEntryRepository.save(entry);
            return ResponseEntity.ok(Map.of("locked", entry.isLocked()));
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/save-entry")
    @ResponseBody
    public ResponseEntity<?> saveEntry(@RequestBody TimetableEntryDTO dto) {
        TimetableEntry entry;
        if (dto.getId() != null) {
            entry = timetableEntryRepository.findById(dto.getId()).orElse(new TimetableEntry());
        } else {
            entry = new TimetableEntry();
        }

        if (dto.getSectionId() != null) {
            sectionRepository.findById(dto.getSectionId()).ifPresent(entry::setSection);
        }
        if (dto.getDayOfWeek() != null) {
            try {
                entry.setDayOfWeek(DayOfWeek.valueOf(dto.getDayOfWeek().toUpperCase()));
            } catch (Exception ignored) {}
        }
        if (dto.getPeriodId() != null) {
            periodRepository.findById(dto.getPeriodId()).ifPresent(entry::setPeriod);
        }
        if (dto.getSubjectId() != null) {
            subjectRepository.findById(dto.getSubjectId()).ifPresent(entry::setSubject);
        } else {
            entry.setSubject(null);
        }
        if (dto.getTeacherId() != null) {
            teacherRepository.findById(dto.getTeacherId()).ifPresent(entry::setTeacher);
        } else {
            entry.setTeacher(null);
        }
        if (dto.getClassroomId() != null) {
            classroomRepository.findById(dto.getClassroomId()).ifPresent(entry::setClassroom);
        } else {
            entry.setClassroom(null);
        }
        if (dto.getStatus() != null) {
            entry.setStatus(dto.getStatus());
        }

        TimetableEntry saved = timetableEntryRepository.save(entry);
        return ResponseEntity.ok(Map.of("success", true, "id", saved.getId()));
    }

    @GetMapping("/teacher/{teacherId}")
    public String teacherTimetable(@PathVariable Long teacherId, Model model) {
        User user = getCurrentUser();
        Teacher teacher = teacherRepository.findById(teacherId).orElseThrow();

        model.addAttribute("departments", departmentRepository.findAll());
        model.addAttribute("years", academicYearRepository.findAll());
        model.addAttribute("sections", sectionRepository.findAll());
        model.addAttribute("teachers", teacherRepository.findAll());
        model.addAttribute("selectedTeacher", teacher);
        model.addAttribute("weeklyTimetable", timetableService.getWeeklyTeacherTimetable(teacher));
        model.addAttribute("allPeriods", timetableService.getAllPeriods());
        model.addAttribute("unreadCount", notificationService.getUnreadCount(user));

        return "admin/timetables";
    }
}
