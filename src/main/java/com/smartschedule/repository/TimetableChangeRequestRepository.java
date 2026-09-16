package com.smartschedule.repository;

import com.smartschedule.model.Teacher;
import com.smartschedule.model.TimetableChangeRequest;
import com.smartschedule.model.enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TimetableChangeRequestRepository extends JpaRepository<TimetableChangeRequest, Long> {
    List<TimetableChangeRequest> findByTeacherOrderByCreatedAtDesc(Teacher teacher);
    List<TimetableChangeRequest> findByStatusOrderByCreatedAtDesc(RequestStatus status);
    List<TimetableChangeRequest> findAllByOrderByCreatedAtDesc();
    long countByStatus(RequestStatus status);
}
