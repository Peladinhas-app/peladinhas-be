package com.peladinhas.backend.domains.bookings.persistence;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<BookingEntity, UUID> {

    @Query("""
            select count(booking) > 0
            from BookingEntity booking
            where booking.pitch.id = :pitchId
              and booking.status = com.peladinhas.backend.domains.bookings.persistence.BookingStatus.CONFIRMED
              and booking.startsAt < :endsAt
              and booking.endsAt > :startsAt
            """)
    boolean existsOverlappingConfirmedBooking(
            @Param("pitchId") UUID pitchId,
            @Param("startsAt") OffsetDateTime startsAt,
            @Param("endsAt") OffsetDateTime endsAt);
}
