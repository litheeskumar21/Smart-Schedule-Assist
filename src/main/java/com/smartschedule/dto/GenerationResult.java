package com.smartschedule.dto;

import com.smartschedule.model.TimetableEntry;
import java.util.ArrayList;
import java.util.List;

public class GenerationResult {
    private List<TimetableEntry> entries = new ArrayList<>();
    private int sectionsProcessed;
    private int classesScheduled;
    private int conflicts;
    private int unassignedPeriods;
    private List<String> messages = new ArrayList<>();
    private List<String> conflictDiagnoses = new ArrayList<>();
    private String solverStatus = "FEASIBLE"; // OPTIMAL, FEASIBLE, INFEASIBLE, TIMEOUT, ERROR
    private String solverEngine = "Java Constraint Solver";
    private boolean valid = true;
    private String aiExplanation;

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

    public List<String> getConflictDiagnoses() {
        return this.conflictDiagnoses;
    }

    public void setConflictDiagnoses(List<String> conflictDiagnoses) {
        this.conflictDiagnoses = conflictDiagnoses;
    }

    public String getSolverStatus() {
        return this.solverStatus;
    }

    public void setSolverStatus(String solverStatus) {
        this.solverStatus = solverStatus;
    }

    public String getSolverEngine() {
        return this.solverEngine;
    }

    public void setSolverEngine(String solverEngine) {
        this.solverEngine = solverEngine;
    }

    public boolean isValid() {
        return this.valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getAiExplanation() {
        return this.aiExplanation;
    }

    public void setAiExplanation(String aiExplanation) {
        this.aiExplanation = aiExplanation;
    }
}
