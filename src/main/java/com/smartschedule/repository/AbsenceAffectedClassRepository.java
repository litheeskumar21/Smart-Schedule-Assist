package com.smartschedule.repository;

import com.smartschedule.model.AbsenceAffectedClass;
import com.smartschedule.model.AbsenceRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AbsenceAffectedClassRepository extends JpaRepository<AbsenceAffectedClass, Long> {
    List<AbsenceAffectedClass> findByAbsenceRequest(AbsenceRequest request);
    List<AbsenceAffectedClass> findByAbsenceRequestId(Long requestId);
}
