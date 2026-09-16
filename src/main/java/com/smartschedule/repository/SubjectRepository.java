package com.smartschedule.repository;

import com.smartschedule.model.AcademicYear;
import com.smartschedule.model.Department;
import com.smartschedule.model.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SubjectRepository extends JpaRepository<Subject, Long> {
    List<Subject> findByDepartmentAndAcademicYear(Department d, AcademicYear y);
    Optional<Subject> findByCode(String code);
    boolean existsByCode(String code);
}
