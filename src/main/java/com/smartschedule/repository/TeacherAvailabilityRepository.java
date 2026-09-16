package com.smartschedule.repository;

import com.smartschedule.model.Period;
import com.smartschedule.model.Teacher;
import com.smartschedule.model.TeacherAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

public interface TeacherAvailabilityRepository extends JpaRepository<TeacherAvailability, Long> {
    List<TeacherAvailability> findByTeacher(Teacher teacher);
    List<TeacherAvailability> findByTeacherAndAvailable(Teacher teacher, boolean available);
    Optional<TeacherAvailability> findByTeacherAndDayOfWeekAndPeriod(Teacher teacher, DayOfWeek dayOfWeek, Period period);
    void deleteByTeacher(Teacher teacher);
}
