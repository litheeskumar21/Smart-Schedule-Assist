package com.smartschedule.dto;

import com.smartschedule.model.enums.SubjectType;
import lombok.Data;

@Data
public class SubjectDTO {
    private String name;
    private String code;
    private SubjectType type;
    private int periodsPerWeek;
    private Long departmentId;
    private Long academicYearId;

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return this.code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public SubjectType getType() {
        return this.type;
    }

    public void setType(SubjectType type) {
        this.type = type;
    }

    public int getPeriodsPerWeek() {
        return this.periodsPerWeek;
    }

    public void setPeriodsPerWeek(int periodsPerWeek) {
        this.periodsPerWeek = periodsPerWeek;
    }

    public Long getDepartmentId() {
        return this.departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public Long getAcademicYearId() {
        return this.academicYearId;
    }

    public void setAcademicYearId(Long academicYearId) {
        this.academicYearId = academicYearId;
    }
}
