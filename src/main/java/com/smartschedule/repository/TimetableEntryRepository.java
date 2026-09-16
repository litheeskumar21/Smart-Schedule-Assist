package com.smartschedule.repository;

import com.smartschedule.model.Classroom;
import com.smartschedule.model.Period;
import com.smartschedule.model.Section;
import com.smartschedule.model.Teacher;
import com.smartschedule.model.TimetableEntry;
import com.smartschedule.model.enums.EntryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

public interface TimetableEntryRepository extends JpaRepository<TimetableEntry, Long> {
    List<TimetableEntry> findBySection(Section section);
    List<TimetableEntry> findBySectionAndPublished(Section s, boolean pub);
    List<TimetableEntry> findBySectionAndPublishedOrderByDayOfWeekAscPeriod_PeriodNumberAsc(Section s, boolean pub);
    List<TimetableEntry> findBySectionAndDayOfWeekAndPublished(Section s, DayOfWeek d, boolean pub);
    List<TimetableEntry> findByTeacherAndPublished(Teacher t, boolean pub);
    List<TimetableEntry> findByTeacherAndPublishedOrderByDayOfWeekAscPeriod_PeriodNumberAsc(Teacher t, boolean pub);
    List<TimetableEntry> findByTeacherAndDayOfWeekAndPublished(Teacher t, DayOfWeek d, boolean pub);
    List<TimetableEntry> findByTeacherAndDayOfWeekAndPublishedTrue(Teacher teacher, DayOfWeek dayOfWeek);
    List<TimetableEntry> findByPublished(boolean pub);
    List<TimetableEntry> findBySectionAndPublishedFalse(Section section);
    List<TimetableEntry> findBySectionAndLockedTrue(Section section);
    
    Optional<TimetableEntry> findBySectionAndDayOfWeekAndPeriod(Section section, DayOfWeek day, Period period);
    Optional<TimetableEntry> findBySectionAndDayOfWeekAndPeriodAndPublished(Section section, DayOfWeek day, Period period, boolean published);
    
    List<TimetableEntry> findByTeacherAndDayOfWeekAndPeriodAndPublishedTrue(Teacher teacher, DayOfWeek day, Period period);
    List<TimetableEntry> findByClassroomAndDayOfWeekAndPeriodAndPublishedTrue(Classroom classroom, DayOfWeek day, Period period);
    List<TimetableEntry> findByDayOfWeekAndPeriodAndPublishedTrue(DayOfWeek day, Period period);
    List<TimetableEntry> findByDayOfWeekAndPeriod(DayOfWeek day, Period period);
    List<TimetableEntry> findByDayOfWeekAndPublishedTrue(DayOfWeek day);
    
    long countByStatusAndDayOfWeekAndPublishedTrue(EntryStatus status, DayOfWeek dayOfWeek);
    
    void deleteBySectionAndPublishedFalse(Section section);
    void deleteBySectionAndPublishedFalseAndLockedFalse(Section section);
}
