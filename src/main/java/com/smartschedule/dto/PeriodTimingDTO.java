package com.smartschedule.dto;

import com.smartschedule.model.enums.PeriodType;
import lombok.Data;

@Data
public class PeriodTimingDTO {
    private int periodNumber;
    private String startTime;
    private String endTime;
    private PeriodType type;

    public int getPeriodNumber() {
        return this.periodNumber;
    }

    public void setPeriodNumber(int periodNumber) {
        this.periodNumber = periodNumber;
    }

    public String getStartTime() {
        return this.startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return this.endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public PeriodType getType() {
        return this.type;
    }

    public void setType(PeriodType type) {
        this.type = type;
    }
}
