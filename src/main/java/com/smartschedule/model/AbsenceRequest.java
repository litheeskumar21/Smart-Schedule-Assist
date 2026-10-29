package com.smartschedule.model;

import com.smartschedule.model.enums.AbsenceType;
import com.smartschedule.model.enums.RequestStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "absence_requests")
@Getter
@Setter
@NoArgsConstructor
public class AbsenceRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    @Column(nullable = false)
    private LocalDate absenceDate;

    @Enumerated(EnumType.STRING)
    private AbsenceType absenceType;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    private RequestStatus status = RequestStatus.PENDING;

    private String adminComment;

    @ManyToOne
    @JoinColumn
    private Teacher suggestedSubstitute;

    @OneToMany(mappedBy = "absenceRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AbsenceAffectedClass> affectedClasses;

    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;

    @ManyToOne
    @JoinColumn
    private User reviewedBy;

    @Column(nullable = false)
    private boolean policyViolated = false;

    @Column(length = 500)
    private String policyViolationReason;

    @Column(nullable = false)
    private boolean escalatedToAdmin = false;

    @Column(nullable = false)
    private boolean adminOverride = false;

    private int weeklyRequestCount = 0;

    private double monthlyLeaveDays = 0.0;

    @Column(columnDefinition = "TEXT")
    private String aiExplanation;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Teacher getTeacher() {
        return this.teacher;
    }

    public void setTeacher(Teacher teacher) {
        this.teacher = teacher;
    }

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

    public String getReason() {
        return this.reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public RequestStatus getStatus() {
        return this.status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public String getAdminComment() {
        return this.adminComment;
    }

    public void setAdminComment(String adminComment) {
        this.adminComment = adminComment;
    }

    public Teacher getSuggestedSubstitute() {
        return this.suggestedSubstitute;
    }

    public void setSuggestedSubstitute(Teacher suggestedSubstitute) {
        this.suggestedSubstitute = suggestedSubstitute;
    }

    public List<AbsenceAffectedClass> getAffectedClasses() {
        return this.affectedClasses;
    }

    public void setAffectedClasses(List<AbsenceAffectedClass> affectedClasses) {
        this.affectedClasses = affectedClasses;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getReviewedAt() {
        return this.reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public User getReviewedBy() {
        return this.reviewedBy;
    }

    public void setReviewedBy(User reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public boolean isPolicyViolated() {
        return this.policyViolated;
    }

    public void setPolicyViolated(boolean policyViolated) {
        this.policyViolated = policyViolated;
    }

    public String getPolicyViolationReason() {
        return this.policyViolationReason;
    }

    public void setPolicyViolationReason(String policyViolationReason) {
        this.policyViolationReason = policyViolationReason;
    }

    public boolean isEscalatedToAdmin() {
        return this.escalatedToAdmin;
    }

    public void setEscalatedToAdmin(boolean escalatedToAdmin) {
        this.escalatedToAdmin = escalatedToAdmin;
    }

    public boolean isAdminOverride() {
        return this.adminOverride;
    }

    public void setAdminOverride(boolean adminOverride) {
        this.adminOverride = adminOverride;
    }

    public int getWeeklyRequestCount() {
        return this.weeklyRequestCount;
    }

    public void setWeeklyRequestCount(int weeklyRequestCount) {
        this.weeklyRequestCount = weeklyRequestCount;
    }

    public double getMonthlyLeaveDays() {
        return this.monthlyLeaveDays;
    }

    public void setMonthlyLeaveDays(double monthlyLeaveDays) {
        this.monthlyLeaveDays = monthlyLeaveDays;
    }

    public String getAiExplanation() {
        return this.aiExplanation;
    }

    public void setAiExplanation(String aiExplanation) {
        this.aiExplanation = aiExplanation;
    }
}
