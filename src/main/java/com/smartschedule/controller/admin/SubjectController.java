package com.smartschedule.controller.admin;

import com.smartschedule.dto.SubjectDTO;
import com.smartschedule.model.*;
import com.smartschedule.model.enums.SubjectType;
import com.smartschedule.repository.*;
import com.smartschedule.service.ActivityLogService;
import com.smartschedule.service.NotificationService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/subjects")
public class SubjectController {

    private final SubjectRepository subjectRepository;
    private final DepartmentRepository departmentRepository;
    private final AcademicYearRepository academicYearRepository;
    private final TeacherRepository teacherRepository;
    private final SectionRepository sectionRepository;
    private final SubjectAssignmentRepository subjectAssignmentRepository;
    private final ActivityLogService activityLogService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public SubjectController(SubjectRepository subjectRepository,
                             DepartmentRepository departmentRepository,
                             AcademicYearRepository academicYearRepository,
                             TeacherRepository teacherRepository,
                             SectionRepository sectionRepository,
                             SubjectAssignmentRepository subjectAssignmentRepository,
                             ActivityLogService activityLogService,
                             NotificationService notificationService,
                             UserRepository userRepository) {
        this.subjectRepository = subjectRepository;
        this.departmentRepository = departmentRepository;
        this.academicYearRepository = academicYearRepository;
        this.teacherRepository = teacherRepository;
        this.sectionRepository = sectionRepository;
        this.subjectAssignmentRepository = subjectAssignmentRepository;
        this.activityLogService = activityLogService;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByUsername(auth.getName()).orElseThrow();
    }

    @GetMapping
    public String listSubjects(Model model) {
        User user = getCurrentUser();
        model.addAttribute("subjects", subjectRepository.findAll());
        model.addAttribute("departments", departmentRepository.findAll());
        model.addAttribute("years", academicYearRepository.findAll());
        model.addAttribute("teachers", teacherRepository.findAll());
        model.addAttribute("sections", sectionRepository.findAll());
        model.addAttribute("assignments", subjectAssignmentRepository.findAll());
        model.addAttribute("unreadCount", notificationService.getUnreadCount(user));
        return "admin/subjects";
    }

    @PostMapping
    public String createSubject(@ModelAttribute SubjectDTO dto, RedirectAttributes redirectAttributes) {
        User user = getCurrentUser();
        if (subjectRepository.existsByCode(dto.getCode().trim().toUpperCase())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Subject code already exists: " + dto.getCode());
            return "redirect:/admin/subjects";
        }

        Subject subject = new Subject();
        subject.setName(dto.getName().trim());
        subject.setCode(dto.getCode().trim().toUpperCase());
        subject.setType(dto.getType() != null ? dto.getType() : SubjectType.THEORY);
        subject.setPeriodsPerWeek(dto.getPeriodsPerWeek() > 0 ? dto.getPeriodsPerWeek() : 3);
        if (dto.getDepartmentId() != null) {
            subject.setDepartment(departmentRepository.findById(dto.getDepartmentId()).orElse(null));
        }
        if (dto.getAcademicYearId() != null) {
            subject.setAcademicYear(academicYearRepository.findById(dto.getAcademicYearId()).orElse(null));
        }

        subjectRepository.save(subject);
        activityLogService.log("CREATE_SUBJECT", "Subject", subject.getId(), user, "Created subject: " + subject.getCode() + " - " + subject.getName());
        redirectAttributes.addFlashAttribute("successMessage", "Subject created successfully.");
        return "redirect:/admin/subjects";
    }

    @GetMapping("/edit/{id}")
    public String editSubjectForm(@PathVariable Long id, Model model) {
        User user = getCurrentUser();
        model.addAttribute("editSubject", subjectRepository.findById(id).orElseThrow());
        model.addAttribute("subjects", subjectRepository.findAll());
        model.addAttribute("departments", departmentRepository.findAll());
        model.addAttribute("years", academicYearRepository.findAll());
        model.addAttribute("teachers", teacherRepository.findAll());
        model.addAttribute("sections", sectionRepository.findAll());
        model.addAttribute("assignments", subjectAssignmentRepository.findAll());
        model.addAttribute("unreadCount", notificationService.getUnreadCount(user));
        return "admin/subjects";
    }

    @PostMapping("/edit/{id}")
    public String updateSubject(@PathVariable Long id, @ModelAttribute SubjectDTO dto, RedirectAttributes redirectAttributes) {
        User user = getCurrentUser();
        Subject subject = subjectRepository.findById(id).orElseThrow();
        subject.setName(dto.getName().trim());
        subject.setCode(dto.getCode().trim().toUpperCase());
        subject.setType(dto.getType() != null ? dto.getType() : SubjectType.THEORY);
        subject.setPeriodsPerWeek(dto.getPeriodsPerWeek() > 0 ? dto.getPeriodsPerWeek() : 3);
        if (dto.getDepartmentId() != null) {
            subject.setDepartment(departmentRepository.findById(dto.getDepartmentId()).orElse(null));
        }
        if (dto.getAcademicYearId() != null) {
            subject.setAcademicYear(academicYearRepository.findById(dto.getAcademicYearId()).orElse(null));
        }

        subjectRepository.save(subject);
        activityLogService.log("UPDATE_SUBJECT", "Subject", subject.getId(), user, "Updated subject: " + subject.getCode());
        redirectAttributes.addFlashAttribute("successMessage", "Subject updated successfully.");
        return "redirect:/admin/subjects";
    }

    @PostMapping("/delete/{id}")
    public String deleteSubject(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        User user = getCurrentUser();
        try {
            Subject subject = subjectRepository.findById(id).orElseThrow();
            subjectRepository.delete(subject);
            activityLogService.log("DELETE_SUBJECT", "Subject", id, user, "Deleted subject: " + subject.getCode());
            redirectAttributes.addFlashAttribute("successMessage", "Subject deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Cannot delete subject as it is referenced by timetables or assignments.");
        }
        return "redirect:/admin/subjects";
    }

    @PostMapping("/assign")
    public String assignSubject(@RequestParam Long subjectId,
                                @RequestParam Long teacherId,
                                @RequestParam Long sectionId,
                                RedirectAttributes redirectAttributes) {
        User user = getCurrentUser();
        Subject subject = subjectRepository.findById(subjectId).orElseThrow();
        Teacher teacher = teacherRepository.findById(teacherId).orElseThrow();
        Section section = sectionRepository.findById(sectionId).orElseThrow();

        SubjectAssignment assignment = subjectAssignmentRepository.findBySubjectAndSection(subject, section)
                .orElse(new SubjectAssignment());
        assignment.setSubject(subject);
        assignment.setTeacher(teacher);
        assignment.setSection(section);
        subjectAssignmentRepository.save(assignment);

        activityLogService.log("ASSIGN_SUBJECT", "SubjectAssignment", assignment.getId(), user,
                "Assigned " + subject.getCode() + " to " + teacher.getUser().getFullName() + " for " + section.getDisplayName());
        redirectAttributes.addFlashAttribute("successMessage", "Subject assigned successfully.");
        return "redirect:/admin/subjects";
    }
}
