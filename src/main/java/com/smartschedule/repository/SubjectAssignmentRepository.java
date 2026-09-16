package com.smartschedule.repository;

import com.smartschedule.model.Section;
import com.smartschedule.model.Subject;
import com.smartschedule.model.SubjectAssignment;
import com.smartschedule.model.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SubjectAssignmentRepository extends JpaRepository<SubjectAssignment, Long> {
    List<SubjectAssignment> findBySection(Section section);
    List<SubjectAssignment> findByTeacher(Teacher teacher);
    Optional<SubjectAssignment> findBySubjectAndSection(Subject s, Section sec);
}
