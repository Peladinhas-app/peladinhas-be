package com.peladinhas.backend.domains.pitches.persistence;

import java.time.LocalTime;

import com.peladinhas.backend.shared.persistence.AbstractUuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "pitch_schedules")
public class PitchScheduleEntity extends AbstractUuidEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pitch_id", nullable = false)
    private PitchEntity pitch;

    @Column(name = "day_of_week", nullable = false)
    private Short dayOfWeek;

    @Column(name = "starts_at", nullable = false, columnDefinition = "time")
    private LocalTime startsAt;

    @Column(name = "ends_at", nullable = false, columnDefinition = "time")
    private LocalTime endsAt;

    public PitchEntity getPitch() {
        return pitch;
    }

    public void setPitch(final PitchEntity pitch) {
        this.pitch = pitch;
    }

    public Short getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(final Short dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public LocalTime getStartsAt() {
        return startsAt;
    }

    public void setStartsAt(final LocalTime startsAt) {
        this.startsAt = startsAt;
    }

    public LocalTime getEndsAt() {
        return endsAt;
    }

    public void setEndsAt(final LocalTime endsAt) {
        this.endsAt = endsAt;
    }
}
