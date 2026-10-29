package com.smartschedule.repository;

import com.smartschedule.model.AbsenceRequest;
import com.smartschedule.model.Teacher;
import com.smartschedule.model.enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface AbsenceRequestRepository extends JpaRepository<AbsenceRequest, Long> {
    List<AbsenceRequest> findByTeacher(Teacher teacher);
    List<AbsenceRequest> findByStatus(RequestStatus status);
    List<AbsenceRequest> findByStatusOrderByCreatedAtDesc(RequestStatus status);
    List<AbsenceRequest> findAllByOrderByCreatedAtDesc();

    List<AbsenceRequest> findByTeacherAndAbsenceDate(Teacher teacher, LocalDate absenceDate);

    List<AbsenceRequest> findByTeacherAndAbsenceDateBetweenAndStatusIn(
            Teacher teacher, LocalDate start, LocalDate end, Collection<RequestStatus> statuses);

    @Query("SELECT r FROM AbsenceRequest r WHERE r.teacher = :teacher AND r.status IN :statuses")
    List<AbsenceRequest> findActiveRequestsByTeacher(
            @Param("teacher") Teacher teacher,
            @Param("statuses") Collection<RequestStatus> statuses);
}
