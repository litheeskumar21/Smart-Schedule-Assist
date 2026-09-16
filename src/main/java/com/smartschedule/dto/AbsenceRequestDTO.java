package com.smartschedule.dto;

import com.smartschedule.model.enums.AbsenceType;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class AbsenceRequestDTO {
    private LocalDate absenceDate;
    private AbsenceType absenceType;
    private List<Long> periodIds;
    private String reason;
    private Long suggestedSubstituteId;

    public LocalDate getAbsenceDate() {
        return this.absenceDate;
    }

    public void setAbsenceDate(LocalDate absenceDate) {
        this.absenceDate = absenceDate;
    }

    public AbsenceType getAbsenceType() {
        return this.absenceType;
    }

    public void setAbsenceType(AbsenceType absenceType) {
        this.absenceType = absenceType;
    }

    public List<Long> getPeriodIds() {
        return this.periodIds;
    }

    public void setPeriodIds(List<Long> periodIds) {
        this.periodIds = periodIds;
    }

    public String getReason() {
        return this.reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Long getSuggestedSubstituteId() {
        return this.suggestedSubstituteId;
    }

    public void setSuggestedSubstituteId(Long suggestedSubstituteId) {
        this.suggestedSubstituteId = suggestedSubstituteId;
    }
}
