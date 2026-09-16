package com.smartschedule.dto;

import lombok.Data;
import java.util.List;

@Data
public class SystemSettingsDTO {
    private String currentSemester;
    private String academicYear;
    private List<PeriodTimingDTO> periodTimings;

    public String getCurrentSemester() {
        return this.currentSemester;
    }

    public void setCurrentSemester(String currentSemester) {
        this.currentSemester = currentSemester;
    }

    public String getAcademicYear() {
        return this.academicYear;
    }

    public void setAcademicYear(String academicYear) {
        this.academicYear = academicYear;
    }

    public List<PeriodTimingDTO> getPeriodTimings() {
        return this.periodTimings;
    }

    public void setPeriodTimings(List<PeriodTimingDTO> periodTimings) {
        this.periodTimings = periodTimings;
    }
}
