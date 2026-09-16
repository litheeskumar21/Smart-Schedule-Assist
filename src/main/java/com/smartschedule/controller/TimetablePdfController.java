package com.smartschedule.controller;

import com.smartschedule.model.*;
import com.smartschedule.repository.*;
import com.smartschedule.service.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class TimetablePdfController {

    private final PdfService pdfService;
    private final TimetableService timetableService;
    private final StudentService studentService;
    private final TeacherService teacherService;
    private final DepartmentRepository departmentRepository;
    private final AcademicYearRepository academicYearRepository;
    private final SectionRepository sectionRepository;
    private final UserRepository userRepository;

    public TimetablePdfController(PdfService pdfService,
                                  TimetableService timetableService,
                                  StudentService studentService,
                                  TeacherService teacherService,
                                  DepartmentRepository departmentRepository,
                                  AcademicYearRepository academicYearRepository,
                                  SectionRepository sectionRepository,
                                  UserRepository userRepository) {
        this.pdfService = pdfService;
        this.timetableService = timetableService;
        this.studentService = studentService;
        this.teacherService = teacherService;
        this.departmentRepository = departmentRepository;
        this.academicYearRepository = academicYearRepository;
        this.sectionRepository = sectionRepository;
        this.userRepository = userRepository;
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByUsername(auth.getName()).orElseThrow();
    }

    private ResponseEntity<byte[]> createPdfResponse(byte[] pdf, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/student/timetable/pdf")
    public ResponseEntity<byte[]> studentTimetablePdf() {
        Student student = studentService.getStudentByUser(getCurrentUser());
        if (student.getSection() == null) return ResponseEntity.badRequest().build();

        List<TimetableEntry> entries = timetableService.getTimetableBySection(student.getSection());
        List<Period> periods = timetableService.getAllPeriods();
        byte[] pdf = pdfService.generateSectionTimetablePdf(student.getSection(), entries, periods);

        return createPdfResponse(pdf, "Timetable_" + student.getSection().getDisplayName() + ".pdf");
    }

    @GetMapping("/student/timetable/pdf/today")
    public ResponseEntity<byte[]> studentTodayTimetablePdf() {
        Student student = studentService.getStudentByUser(getCurrentUser());
        if (student.getSection() == null) return ResponseEntity.badRequest().build();

        DayOfWeek currentDay = LocalDate.now().getDayOfWeek();
        List<TimetableEntry> entries = timetableService.getTimetableBySectionAndDay(student.getSection(), currentDay);
        List<Period> periods = timetableService.getAllPeriods();
        byte[] pdf = pdfService.generateSectionTimetablePdf(student.getSection(), entries, periods);

        return createPdfResponse(pdf, "Today_Timetable_" + student.getSection().getDisplayName() + ".pdf");
    }

    @GetMapping("/teacher/timetable/pdf")
    public ResponseEntity<byte[]> teacherTimetablePdf() {
        Teacher teacher = teacherService.getTeacherByUser(getCurrentUser());
        List<TimetableEntry> entries = timetableService.getTimetableByTeacher(teacher);
        List<Period> periods = timetableService.getAllPeriods();
        byte[] pdf = pdfService.generateTeacherTimetablePdf(teacher, entries, periods);

        return createPdfResponse(pdf, "Teacher_Schedule_" + teacher.getTeacherId() + ".pdf");
    }

    @GetMapping("/admin/timetables/pdf/{sectionId}")
    public ResponseEntity<byte[]> sectionTimetablePdf(@PathVariable Long sectionId) {
        Section section = sectionRepository.findById(sectionId).orElseThrow();
        List<TimetableEntry> entries = timetableService.getTimetableBySection(section);
        List<Period> periods = timetableService.getAllPeriods();
        byte[] pdf = pdfService.generateSectionTimetablePdf(section, entries, periods);

        return createPdfResponse(pdf, "Timetable_" + section.getDisplayName() + ".pdf");
    }

    @GetMapping("/admin/timetables/pdf/teacher/{teacherId}")
    public ResponseEntity<byte[]> adminTeacherTimetablePdf(@PathVariable Long teacherId) {
        Teacher teacher = teacherService.getTeacherById(teacherId);
        List<TimetableEntry> entries = timetableService.getTimetableByTeacher(teacher);
        List<Period> periods = timetableService.getAllPeriods();
        byte[] pdf = pdfService.generateTeacherTimetablePdf(teacher, entries, periods);

        return createPdfResponse(pdf, "Teacher_Schedule_" + teacher.getTeacherId() + ".pdf");
    }

    @GetMapping("/admin/timetables/pdf/department/{deptId}")
    public ResponseEntity<byte[]> departmentTimetablePdf(@PathVariable Long deptId) {
        Department dept = departmentRepository.findById(deptId).orElseThrow();
        AcademicYear yr2 = academicYearRepository.findAll().stream().findFirst().orElse(null);
        List<Section> sections = sectionRepository.findByDepartment(dept);

        Map<Long, List<TimetableEntry>> sectionEntriesMap = new HashMap<>();
        for (Section sec : sections) {
            sectionEntriesMap.put(sec.getId(), timetableService.getTimetableBySection(sec));
        }

        List<Period> periods = timetableService.getAllPeriods();
        byte[] pdf = pdfService.generateDepartmentTimetablePdf(dept, yr2 != null ? yr2 : new AcademicYear(), sections, sectionEntriesMap, periods);

        return createPdfResponse(pdf, "Department_" + dept.getCode() + "_Timetable.pdf");
    }
}
