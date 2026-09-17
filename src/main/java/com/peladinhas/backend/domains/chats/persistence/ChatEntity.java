package com.peladinhas.backend.domains.chats.persistence;

import java.time.OffsetDateTime;

import com.peladinhas.backend.domains.bookings.persistence.BookingEntity;
import com.peladinhas.backend.domains.groups.persistence.GroupEntity;
import com.peladinhas.backend.domains.matches.persistence.MatchEntity;
import com.peladinhas.backend.shared.persistence.AbstractUuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "chats")
public class ChatEntity extends AbstractUuidEntity {

    @Convert(converter = ChatType.ConverterImpl.class)
    @Column(name = "type", nullable = false, columnDefinition = "varchar")
    private ChatType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private GroupEntity group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id")
    private MatchEntity match;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private BookingEntity booking;

    @Column(name = "created_at", nullable = false, columnDefinition = "timestamptz")
    private OffsetDateTime createdAt;

    public ChatType getType() {
        return type;
    }

    public void setType(final ChatType type) {
        this.type = type;
    }

    public GroupEntity getGroup() {
        return group;
    }

    public void setGroup(final GroupEntity group) {
        this.group = group;
    }

    public MatchEntity getMatch() {
        return match;
    }

    public void setMatch(final MatchEntity match) {
        this.match = match;
    }

    public BookingEntity getBooking() {
        return booking;
    }

    public void setBooking(final BookingEntity booking) {
        this.booking = booking;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
