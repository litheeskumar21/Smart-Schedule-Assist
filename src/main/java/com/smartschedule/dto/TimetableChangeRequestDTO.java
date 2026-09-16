package com.smartschedule.dto;

import com.smartschedule.model.enums.ChangeType;
import lombok.Data;

@Data
public class TimetableChangeRequestDTO {
    private Long timetableEntryId;
    private ChangeType changeType;
    private String requestedChanges;
    private String reason;

    public Long getTimetableEntryId() {
        return this.timetableEntryId;
    }

    public void setTimetableEntryId(Long timetableEntryId) {
        this.timetableEntryId = timetableEntryId;
    }

    public ChangeType getChangeType() {
        return this.changeType;
    }

    public void setChangeType(ChangeType changeType) {
        this.changeType = changeType;
    }

    public String getRequestedChanges() {
        return this.requestedChanges;
    }

    public void setRequestedChanges(String requestedChanges) {
        this.requestedChanges = requestedChanges;
    }

    public String getReason() {
        return this.reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
