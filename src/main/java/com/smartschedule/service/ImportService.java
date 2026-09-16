package com.smartschedule.service;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import com.smartschedule.dto.*;
import com.smartschedule.model.*;
import com.smartschedule.model.enums.RoomType;
import com.smartschedule.model.enums.SubjectType;
import com.smartschedule.repository.*;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.*;

@Service
@Transactional
public class ImportService {
    private final StudentService studentService;
    private final TeacherService teacherService;
    private final SubjectRepository subjectRepository;
    private final ClassroomRepository classroomRepository;
    private final DepartmentRepository departmentRepository;
    private final AcademicYearRepository academicYearRepository;
    private final SectionRepository sectionRepository;

    public ImportService(StudentService studentService,
                         TeacherService teacherService,
                         SubjectRepository subjectRepository,
                         ClassroomRepository classroomRepository,
                         DepartmentRepository departmentRepository,
                         AcademicYearRepository academicYearRepository,
                         SectionRepository sectionRepository) {
        this.studentService = studentService;
        this.teacherService = teacherService;
        this.subjectRepository = subjectRepository;
        this.classroomRepository = classroomRepository;
        this.departmentRepository = departmentRepository;
        this.academicYearRepository = academicYearRepository;
        this.sectionRepository = sectionRepository;
    }

    public ImportResult importStudentsFromFile(MultipartFile file) {
        ImportResult result = new ImportResult();
        List<String[]> rows = parseFile(file, result);
        if (rows.isEmpty()) return result;

        Set<String> seenIds = new HashSet<>();
        int rowNum = 1; // Row 1 is header

        for (int i = 1; i < rows.size(); i++) {
            rowNum++;
            String[] row = rows.get(i);
            if (row.length < 3 || isRowEmpty(row)) continue;

            String studentId = getVal(row, 0);
            String fullName = getVal(row, 1);
            String email = getVal(row, 2);
            String password = getVal(row, 3);
            String deptCode = getVal(row, 4);
            String yearNumStr = getVal(row, 5);
            String sectionName = getVal(row, 6);
            String semesterStr = getVal(row, 7);

            if (studentId.isEmpty() || fullName.isEmpty()) {
                result.getErrors().add("Row " + rowNum + ": Student ID and Full Name are required.");
                result.setErrorCount(result.getErrorCount() + 1);
                continue;
            }

            if (!seenIds.add(studentId)) {
                result.getErrors().add("Row " + rowNum + ": Duplicate Student ID in file: " + studentId);
                result.setErrorCount(result.getErrorCount() + 1);
                continue;
            }

            try {
                StudentDTO dto = new StudentDTO();
                dto.setStudentId(studentId);
                dto.setFullName(fullName);
                dto.setEmail(email.isEmpty() ? null : email);
                dto.setPassword(password.isEmpty() ? "student123" : password);

                if (!deptCode.isEmpty()) {
                    Department dept = departmentRepository.findByCode(deptCode)
                            .orElse(null);
                    if (dept != null) dto.setDepartmentId(dept.getId());
                }

                if (!yearNumStr.isEmpty()) {
                    try {
                        int yr = Integer.parseInt(yearNumStr);
                        academicYearRepository.findAll().stream()
                                .filter(y -> y.getYearNumber() == yr)
                                .findFirst()
                                .ifPresent(y -> dto.setAcademicYearId(y.getId()));
                    } catch (NumberFormatException ignored) {}
                }

                if (!sectionName.isEmpty() && dto.getDepartmentId() != null && dto.getAcademicYearId() != null) {
                    Department dept = departmentRepository.findById(dto.getDepartmentId()).orElse(null);
                    AcademicYear yr = academicYearRepository.findById(dto.getAcademicYearId()).orElse(null);
                    if (dept != null && yr != null) {
                        sectionRepository.findByDepartmentAndAcademicYearAndName(dept, yr, sectionName)
                                .ifPresent(sec -> dto.setSectionId(sec.getId()));
                    }
                }

                int sem = 1;
                if (!semesterStr.isEmpty()) {
                    try { sem = Integer.parseInt(semesterStr); } catch (NumberFormatException ignored) {}
                }
                dto.setSemester(sem);

                studentService.createStudent(dto);
                result.setSuccessCount(result.getSuccessCount() + 1);
            } catch (Exception e) {
                result.getErrors().add("Row " + rowNum + " (" + studentId + "): " + e.getMessage());
                result.setErrorCount(result.getErrorCount() + 1);
            }
        }
        return result;
    }

    public ImportResult importTeachersFromFile(MultipartFile file) {
        ImportResult result = new ImportResult();
        List<String[]> rows = parseFile(file, result);
        if (rows.isEmpty()) return result;

        Set<String> seenIds = new HashSet<>();
        int rowNum = 1;

        for (int i = 1; i < rows.size(); i++) {
            rowNum++;
            String[] row = rows.get(i);
            if (row.length < 2 || isRowEmpty(row)) continue;

            String teacherId = getVal(row, 0);
            String fullName = getVal(row, 1);
            String email = getVal(row, 2);
            String password = getVal(row, 3);
            String phone = getVal(row, 4);
            String deptCodesStr = getVal(row, 5);

            if (teacherId.isEmpty() || fullName.isEmpty()) {
                result.getErrors().add("Row " + rowNum + ": Teacher ID and Full Name are required.");
                result.setErrorCount(result.getErrorCount() + 1);
                continue;
            }

            if (!seenIds.add(teacherId)) {
                result.getErrors().add("Row " + rowNum + ": Duplicate Teacher ID in file: " + teacherId);
                result.setErrorCount(result.getErrorCount() + 1);
                continue;
            }

            try {
                TeacherDTO dto = new TeacherDTO();
                dto.setTeacherId(teacherId);
                dto.setFullName(fullName);
                dto.setEmail(email.isEmpty() ? null : email);
                dto.setPassword(password.isEmpty() ? "teacher123" : password);
                dto.setPhone(phone.isEmpty() ? null : phone);

                List<Long> deptIds = new ArrayList<>();
                if (!deptCodesStr.isEmpty()) {
                    String[] codes = deptCodesStr.split("[,;]");
                    for (String c : codes) {
                        departmentRepository.findByCode(c.trim())
                                .ifPresent(d -> deptIds.add(d.getId()));
                    }
                }
                dto.setDepartmentIds(deptIds);

                teacherService.createTeacher(dto);
                result.setSuccessCount(result.getSuccessCount() + 1);
            } catch (Exception e) {
                result.getErrors().add("Row " + rowNum + " (" + teacherId + "): " + e.getMessage());
                result.setErrorCount(result.getErrorCount() + 1);
            }
        }
        return result;
    }

    public ImportResult importSubjectsFromFile(MultipartFile file) {
        ImportResult result = new ImportResult();
        List<String[]> rows = parseFile(file, result);
        if (rows.isEmpty()) return result;

        int rowNum = 1;
        for (int i = 1; i < rows.size(); i++) {
            rowNum++;
            String[] row = rows.get(i);
            if (row.length < 2 || isRowEmpty(row)) continue;

            String code = getVal(row, 0);
            String name = getVal(row, 1);
            String typeStr = getVal(row, 2);
            String periodsStr = getVal(row, 3);
            String deptCode = getVal(row, 4);
            String yearNumStr = getVal(row, 5);

            if (code.isEmpty() || name.isEmpty()) {
                result.getErrors().add("Row " + rowNum + ": Subject Code and Name are required.");
                result.setErrorCount(result.getErrorCount() + 1);
                continue;
            }

            if (subjectRepository.existsByCode(code)) {
                result.getErrors().add("Row " + rowNum + ": Subject code already exists: " + code);
                result.setErrorCount(result.getErrorCount() + 1);
                continue;
            }

            try {
                Subject subject = new Subject();
                subject.setCode(code);
                subject.setName(name);

                SubjectType type = SubjectType.THEORY;
                if ("LAB".equalsIgnoreCase(typeStr) || "PRACTICAL".equalsIgnoreCase(typeStr)) {
                    type = SubjectType.LAB;
                }
                subject.setType(type);

                int periods = 3;
                if (!periodsStr.isEmpty()) {
                    try { periods = Integer.parseInt(periodsStr); } catch (NumberFormatException ignored) {}
                }
                subject.setPeriodsPerWeek(periods);

                if (!deptCode.isEmpty()) {
                    departmentRepository.findByCode(deptCode).ifPresent(subject::setDepartment);
                }

                if (!yearNumStr.isEmpty()) {
                    try {
                        int yr = Integer.parseInt(yearNumStr);
                        academicYearRepository.findAll().stream()
                                .filter(y -> y.getYearNumber() == yr)
                                .findFirst()
                                .ifPresent(subject::setAcademicYear);
                    } catch (NumberFormatException ignored) {}
                }

                subjectRepository.save(subject);
                result.setSuccessCount(result.getSuccessCount() + 1);
            } catch (Exception e) {
                result.getErrors().add("Row " + rowNum + " (" + code + "): " + e.getMessage());
                result.setErrorCount(result.getErrorCount() + 1);
            }
        }
        return result;
    }

    public ImportResult importClassroomsFromFile(MultipartFile file) {
        ImportResult result = new ImportResult();
        List<String[]> rows = parseFile(file, result);
        if (rows.isEmpty()) return result;

        int rowNum = 1;
        for (int i = 1; i < rows.size(); i++) {
            rowNum++;
            String[] row = rows.get(i);
            if (row.length < 1 || isRowEmpty(row)) continue;

            String name = getVal(row, 0);
            String typeStr = getVal(row, 1);
            String capStr = getVal(row, 2);

            if (name.isEmpty()) {
                result.getErrors().add("Row " + rowNum + ": Classroom Name is required.");
                result.setErrorCount(result.getErrorCount() + 1);
                continue;
            }

            if (classroomRepository.findByName(name).isPresent()) {
                result.getErrors().add("Row " + rowNum + ": Classroom already exists: " + name);
                result.setErrorCount(result.getErrorCount() + 1);
                continue;
            }

            try {
                Classroom room = new Classroom();
                room.setName(name);

                RoomType rtype = RoomType.CLASSROOM;
                if (!typeStr.isEmpty()) {
                    try {
                        rtype = RoomType.valueOf(typeStr.toUpperCase().replace(" ", "_"));
                    } catch (IllegalArgumentException ignored) {}
                }
                room.setRoomType(rtype);

                int cap = 60;
                if (!capStr.isEmpty()) {
                    try { cap = Integer.parseInt(capStr); } catch (NumberFormatException ignored) {}
                }
                room.setCapacity(cap);

                classroomRepository.save(room);
                result.setSuccessCount(result.getSuccessCount() + 1);
            } catch (Exception e) {
                result.getErrors().add("Row " + rowNum + " (" + name + "): " + e.getMessage());
                result.setErrorCount(result.getErrorCount() + 1);
            }
        }
        return result;
    }

    private List<String[]> parseFile(MultipartFile file, ImportResult result) {
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        try {
            if (filename.endsWith(".xlsx") || filename.endsWith(".xls")) {
                return parseExcel(file.getInputStream());
            } else {
                return parseCSV(file.getInputStream());
            }
        } catch (Exception e) {
            result.getErrors().add("Failed to parse file: " + e.getMessage());
            result.setErrorCount(1);
            return List.of();
        }
    }

    private List<String[]> parseCSV(InputStream is) throws IOException, CsvException {
        try (CSVReader reader = new CSVReader(new InputStreamReader(is))) {
            return reader.readAll();
        }
    }

    private List<String[]> parseExcel(InputStream is) throws IOException {
        List<String[]> list = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            for (Row row : sheet) {
                int lastCol = Math.max(row.getLastCellNum(), 0);
                String[] rowData = new String[lastCol];
                for (int c = 0; c < lastCol; c++) {
                    Cell cell = row.getCell(c, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    rowData[c] = cell == null ? "" : formatter.formatCellValue(cell).trim();
                }
                list.add(rowData);
            }
        }
        return list;
    }

    private String getVal(String[] row, int index) {
        if (index < row.length && row[index] != null) {
            return row[index].trim();
        }
        return "";
    }

    private boolean isRowEmpty(String[] row) {
        for (String cell : row) {
            if (cell != null && !cell.trim().isEmpty()) return false;
        }
        return true;
    }
}
