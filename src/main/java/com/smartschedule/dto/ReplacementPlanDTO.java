package com.smartschedule.dto;

import com.smartschedule.model.AbsenceAffectedClass;
import java.util.ArrayList;
import java.util.List;

public class ReplacementPlanDTO {
    private Long absenceRequestId;
    private String teacherName;
    private int totalAffectedClasses;
    private int proposedCount;
    private int unresolvedCount;
    private boolean fullyResolved;
    private List<AbsenceAffectedClass> affectedClasses = new ArrayList<>();
    private List<String> notices = new ArrayList<>();
    private String aiSummary;

    public ReplacementPlanDTO() {}

    public Long getAbsenceRequestId() {
        return this.absenceRequestId;
    }

    public void setAbsenceRequestId(Long absenceRequestId) {
        this.absenceRequestId = absenceRequestId;
    }

    public String getTeacherName() {
        return this.teacherName;
    }

    public void setTeacherName(String teacherName) {
        this.teacherName = teacherName;
    }

    public int getTotalAffectedClasses() {
        return this.totalAffectedClasses;
    }

    public void setTotalAffectedClasses(int totalAffectedClasses) {
        this.totalAffectedClasses = totalAffectedClasses;
    }

    public int getProposedCount() {
        return this.proposedCount;
    }

    public void setProposedCount(int proposedCount) {
        this.proposedCount = proposedCount;
    }

    public int getUnresolvedCount() {
        return this.unresolvedCount;
    }

    public void setUnresolvedCount(int unresolvedCount) {
        this.unresolvedCount = unresolvedCount;
    }

    public boolean isFullyResolved() {
        return this.fullyResolved;
    }

    public void setFullyResolved(boolean fullyResolved) {
        this.fullyResolved = fullyResolved;
    }

    public List<AbsenceAffectedClass> getAffectedClasses() {
        return this.affectedClasses;
    }

    public void setAffectedClasses(List<AbsenceAffectedClass> affectedClasses) {
        this.affectedClasses = affectedClasses;
    }

    public List<String> getNotices() {
        return this.notices;
    }

    public void setNotices(List<String> notices) {
        this.notices = notices;
    }

    public String getAiSummary() {
        return this.aiSummary;
    }

    public void setAiSummary(String aiSummary) {
        this.aiSummary = aiSummary;
    }
}
