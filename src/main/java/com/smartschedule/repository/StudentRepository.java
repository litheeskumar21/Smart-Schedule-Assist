package com.smartschedule.repository;

import com.smartschedule.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByStudentId(String studentId);
    Optional<Student> findByUser(User user);
    List<Student> findBySection(Section section);
    List<Student> findByDepartment(Department department);
    List<Student> findByDepartmentAndAcademicYearAndSection(Department d, AcademicYear y, Section s);
    boolean existsByStudentId(String studentId);
}
