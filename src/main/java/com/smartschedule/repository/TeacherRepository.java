package com.smartschedule.repository;

import com.smartschedule.model.Department;
import com.smartschedule.model.Teacher;
import com.smartschedule.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {
    Optional<Teacher> findByTeacherId(String teacherId);
    Optional<Teacher> findByUser(User user);
    List<Teacher> findByDepartmentsContaining(Department department);
    boolean existsByTeacherId(String teacherId);
}
