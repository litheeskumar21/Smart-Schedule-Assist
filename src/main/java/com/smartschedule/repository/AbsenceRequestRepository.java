package com.smartschedule.repository;

import com.smartschedule.model.AbsenceRequest;
import com.smartschedule.model.Teacher;
import com.smartschedule.model.enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AbsenceRequestRepository extends JpaRepository<AbsenceRequest, Long> {
    List<AbsenceRequest> findByTeacher(Teacher teacher);
    List<AbsenceRequest> findByStatus(RequestStatus status);
    List<AbsenceRequest> findByStatusOrderByCreatedAtDesc(RequestStatus status);
}
