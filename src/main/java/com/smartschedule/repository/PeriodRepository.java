package com.smartschedule.repository;

import com.smartschedule.model.Period;
import com.smartschedule.model.enums.PeriodType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PeriodRepository extends JpaRepository<Period, Long> {
    List<Period> findByTypeOrderByPeriodNumberAsc(PeriodType type);
    List<Period> findAllByOrderByPeriodNumberAsc();
    Optional<Period> findByPeriodNumber(int n);
}
