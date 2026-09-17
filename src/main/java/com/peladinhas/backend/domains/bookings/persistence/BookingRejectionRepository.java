package com.peladinhas.backend.domains.bookings.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRejectionRepository extends JpaRepository<BookingRejectionEntity, UUID> {
}
