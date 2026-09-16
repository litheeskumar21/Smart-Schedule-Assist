package com.smartschedule.dto;

import com.smartschedule.model.enums.TargetType;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class AnnouncementDTO {
    private String title;
    private String content;
    private TargetType targetType;
    private List<Long> targetIds;
    private LocalDateTime scheduledAt;

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return this.content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public TargetType getTargetType() {
        return this.targetType;
    }

    public void setTargetType(TargetType targetType) {
        this.targetType = targetType;
    }

    public List<Long> getTargetIds() {
        return this.targetIds;
    }

    public void setTargetIds(List<Long> targetIds) {
        this.targetIds = targetIds;
    }

    public LocalDateTime getScheduledAt() {
        return this.scheduledAt;
    }

    public void setScheduledAt(LocalDateTime scheduledAt) {
        this.scheduledAt = scheduledAt;
    }
}
