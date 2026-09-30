package com.peladinhas.backend.domains.bookings.persistence;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<BookingEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from BookingEntity b join fetch b.match join fetch b.pitch where b.id = :id")
    Optional<BookingEntity> findByIdForUpdate(@Param("id") UUID id);

    @EntityGraph(attributePaths = {"match", "pitch", "pitch.ownerUser"})
    List<BookingEntity> findAllByPitchOwnerUserIdOrderByCreatedAtDesc(UUID ownerUserId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select booking
            from BookingEntity booking
            where booking.pitch.id = :pitchId
              and booking.status = com.peladinhas.backend.domains.bookings.persistence.BookingStatus.PROVISIONAL
              and booking.id <> :excludedBookingId
              and booking.startsAt < :endsAt
              and booking.endsAt > :startsAt
            """)
    List<BookingEntity> findOverlappingProvisionalCompetitorsForUpdate(
            @Param("pitchId") UUID pitchId,
            @Param("excludedBookingId") UUID excludedBookingId,
            @Param("startsAt") OffsetDateTime startsAt,
            @Param("endsAt") OffsetDateTime endsAt);

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
