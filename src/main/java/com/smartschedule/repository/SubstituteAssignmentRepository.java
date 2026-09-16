package com.smartschedule.repository;

import com.smartschedule.model.AbsenceAffectedClass;
import com.smartschedule.model.SubstituteAssignment;
import com.smartschedule.model.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SubstituteAssignmentRepository extends JpaRepository<SubstituteAssignment, Long> {
    Optional<SubstituteAssignment> findByAffectedClass(AbsenceAffectedClass affectedClass);
    List<SubstituteAssignment> findBySubstituteTeacher(Teacher substituteTeacher);
}
