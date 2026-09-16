package com.smartschedule.model;

import com.smartschedule.model.enums.ClassResolution;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "absence_affected_classes")
@Getter
@Setter
@NoArgsConstructor
public class AbsenceAffectedClass {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(nullable = false)
    private AbsenceRequest absenceRequest;

    @ManyToOne
    @JoinColumn(nullable = false)
    private TimetableEntry timetableEntry;

    @ManyToOne
    @JoinColumn(nullable = false)
    private Period period;

    @Enumerated(EnumType.STRING)
    private ClassResolution resolution = ClassResolution.PENDING;

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AbsenceRequest getAbsenceRequest() {
        return this.absenceRequest;
    }

    public void setAbsenceRequest(AbsenceRequest absenceRequest) {
        this.absenceRequest = absenceRequest;
    }

    public TimetableEntry getTimetableEntry() {
        return this.timetableEntry;
    }

    public void setTimetableEntry(TimetableEntry timetableEntry) {
        this.timetableEntry = timetableEntry;
    }

    public Period getPeriod() {
        return this.period;
    }

    public void setPeriod(Period period) {
        this.period = period;
    }

    public ClassResolution getResolution() {
        return this.resolution;
    }

    public void setResolution(ClassResolution resolution) {
        this.resolution = resolution;
    }
}
