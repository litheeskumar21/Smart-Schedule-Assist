package com.smartschedule.repository;

import com.smartschedule.model.AcademicYear;
import com.smartschedule.model.Department;
import com.smartschedule.model.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SectionRepository extends JpaRepository<Section, Long> {
    List<Section> findByDepartment(Department department);
    List<Section> findByDepartmentAndAcademicYear(Department d, AcademicYear y);
    Optional<Section> findByDepartmentAndAcademicYearAndName(Department d, AcademicYear y, String name);
}
