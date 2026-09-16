package com.smartschedule.dto;

import com.smartschedule.model.TimetableEntry;
import lombok.Data;
import java.util.List;

@Data
public class GenerationResult {
    private List<TimetableEntry> entries;
    private int sectionsProcessed;
    private int classesScheduled;
    private int conflicts;
    private int unassignedPeriods;
    private List<String> messages;

    public List<TimetableEntry> getEntries() {
        return this.entries;
    }

    public void setEntries(List<TimetableEntry> entries) {
        this.entries = entries;
    }

    public int getSectionsProcessed() {
        return this.sectionsProcessed;
    }

    public void setSectionsProcessed(int sectionsProcessed) {
        this.sectionsProcessed = sectionsProcessed;
    }

    public int getClassesScheduled() {
        return this.classesScheduled;
    }

    public void setClassesScheduled(int classesScheduled) {
        this.classesScheduled = classesScheduled;
    }

    public int getConflicts() {
        return this.conflicts;
    }

    public void setConflicts(int conflicts) {
        this.conflicts = conflicts;
    }

    public int getUnassignedPeriods() {
        return this.unassignedPeriods;
    }

    public void setUnassignedPeriods(int unassignedPeriods) {
        this.unassignedPeriods = unassignedPeriods;
    }

    public List<String> getMessages() {
        return this.messages;
    }

    public void setMessages(List<String> messages) {
        this.messages = messages;
    }
}
