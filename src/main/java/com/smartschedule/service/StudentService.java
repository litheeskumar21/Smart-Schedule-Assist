package com.smartschedule.service;

import com.smartschedule.dto.StudentDTO;
import com.smartschedule.exception.DuplicateResourceException;
import com.smartschedule.exception.ResourceNotFoundException;
import com.smartschedule.model.AcademicYear;
import com.smartschedule.model.Department;
import com.smartschedule.model.Section;
import com.smartschedule.model.Student;
import com.smartschedule.model.User;
import com.smartschedule.model.enums.UserRole;
import com.smartschedule.repository.AcademicYearRepository;
import com.smartschedule.repository.DepartmentRepository;
import com.smartschedule.repository.SectionRepository;
import com.smartschedule.repository.StudentRepository;
import com.smartschedule.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class StudentService {
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final SectionRepository sectionRepository;
    private final DepartmentRepository departmentRepository;
    private final AcademicYearRepository academicYearRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public StudentService(StudentRepository studentRepository, UserRepository userRepository,
                          SectionRepository sectionRepository, DepartmentRepository departmentRepository,
                          AcademicYearRepository academicYearRepository, BCryptPasswordEncoder passwordEncoder) {
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.sectionRepository = sectionRepository;
        this.departmentRepository = departmentRepository;
        this.academicYearRepository = academicYearRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Student getStudentByUser(User user) {
        return studentRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found for user: " + user.getUsername()));
    }

    public Student getStudentByStudentId(String studentId) {
        return studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with ID: " + studentId));
    }

    public Student getStudentById(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + id));
    }

    public List<Student> getStudentsBySection(Section section) {
        return studentRepository.findBySection(section);
    }

    public List<Student> getStudentsByDepartment(Department department) {
        return studentRepository.findByDepartment(department);
    }

    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    public Student createStudent(StudentDTO dto) {
        if (studentRepository.existsByStudentId(dto.getStudentId()) || userRepository.existsByUsername(dto.getStudentId())) {
            throw new DuplicateResourceException("Student ID already exists: " + dto.getStudentId());
        }

        User user = new User();
        user.setUsername(dto.getStudentId());
        String rawPassword = (dto.getPassword() == null || dto.getPassword().trim().isEmpty()) ? "student123" : dto.getPassword();
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(UserRole.ROLE_STUDENT);
        user.setFullName(dto.getFullName());
        user.setEmail(dto.getEmail());
        user.setEnabled(true);
        user = userRepository.save(user);

        Student student = new Student();
        student.setStudentId(dto.getStudentId());
        student.setUser(user);
        if (dto.getDepartmentId() != null) {
            student.setDepartment(departmentRepository.findById(dto.getDepartmentId()).orElse(null));
        }
        if (dto.getAcademicYearId() != null) {
            student.setAcademicYear(academicYearRepository.findById(dto.getAcademicYearId()).orElse(null));
        }
        if (dto.getSectionId() != null) {
            student.setSection(sectionRepository.findById(dto.getSectionId()).orElse(null));
        }
        student.setSemester(dto.getSemester() > 0 ? dto.getSemester() : 1);

        return studentRepository.save(student);
    }

    public Student updateStudent(Long id, StudentDTO dto) {
        Student student = getStudentById(id);
        User user = student.getUser();
        if (user != null) {
            user.setFullName(dto.getFullName());
            user.setEmail(dto.getEmail());
            if (dto.getPassword() != null && !dto.getPassword().trim().isEmpty()) {
                user.setPassword(passwordEncoder.encode(dto.getPassword()));
            }
            userRepository.save(user);
        }

        if (dto.getDepartmentId() != null) {
            student.setDepartment(departmentRepository.findById(dto.getDepartmentId()).orElse(null));
        }
        if (dto.getAcademicYearId() != null) {
            student.setAcademicYear(academicYearRepository.findById(dto.getAcademicYearId()).orElse(null));
        }
        if (dto.getSectionId() != null) {
            student.setSection(sectionRepository.findById(dto.getSectionId()).orElse(null));
        }
        student.setSemester(dto.getSemester() > 0 ? dto.getSemester() : student.getSemester());

        return studentRepository.save(student);
    }

    public void deleteStudent(Long id) {
        Student student = getStudentById(id);
        User user = student.getUser();
        studentRepository.delete(student);
        if (user != null) {
            userRepository.delete(user);
        }
    }

    public long countStudents() {
        return studentRepository.count();
    }
}
