package com.smartschedule.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "substitute_assignments")
@Getter
@Setter
@NoArgsConstructor
public class SubstituteAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(nullable = false)
    private AbsenceAffectedClass affectedClass;

    @ManyToOne
    @JoinColumn(nullable = false)
    private Teacher substituteTeacher;

    @ManyToOne
    @JoinColumn(nullable = false)
    private User assignedBy;

    private LocalDateTime createdAt;

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

    public AbsenceAffectedClass getAffectedClass() {
        return this.affectedClass;
    }

    public void setAffectedClass(AbsenceAffectedClass affectedClass) {
        this.affectedClass = affectedClass;
    }

    public Teacher getSubstituteTeacher() {
        return this.substituteTeacher;
    }

    public void setSubstituteTeacher(Teacher substituteTeacher) {
        this.substituteTeacher = substituteTeacher;
    }

    public User getAssignedBy() {
        return this.assignedBy;
    }

    public void setAssignedBy(User assignedBy) {
        this.assignedBy = assignedBy;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
