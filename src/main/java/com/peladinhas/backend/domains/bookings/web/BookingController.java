package com.peladinhas.backend.domains.bookings.web;

import java.util.UUID;

import com.peladinhas.backend.auth.CurrentUserService;
import com.peladinhas.backend.domains.bookings.persistence.BookingEntity;
import com.peladinhas.backend.domains.bookings.persistence.BookingRejectionReason;
import com.peladinhas.backend.domains.bookings.service.BookingService;
import com.peladinhas.backend.domains.bookings.service.CreateProvisionalBookingCommand;
import com.peladinhas.backend.domains.bookings.service.RejectBookingCommand;
import com.peladinhas.backend.shared.web.ApiEnumParser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final CurrentUserService currentUserService;

    public BookingController(
            final BookingService bookingService,
            final CurrentUserService currentUserService) {
        this.bookingService = bookingService;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse createProvisionalBooking(
            @Valid @RequestBody final CreateProvisionalBookingRequest request) {
        BookingEntity booking = bookingService.createProvisionalBooking(new CreateProvisionalBookingCommand(
                currentUserService.requireCurrentUserId(),
                request.matchId(),
                request.pitchId(),
                request.totalPrice(),
                request.currency()));
        return BookingResponse.from(booking);
    }

    @PostMapping("/{bookingId}/reject")
    public BookingResponse rejectBooking(
            @PathVariable final UUID bookingId,
            @Valid @RequestBody final RejectBookingRequest request) {
        BookingRejectionReason reasonCode = ApiEnumParser.parse(
                BookingRejectionReason.class,
                request.reasonCode(),
                "reasonCode");
        BookingEntity booking = bookingService.rejectBooking(new RejectBookingCommand(
                bookingId,
                currentUserService.requireCurrentUserId(),
                reasonCode,
                request.explanation()));
        return BookingResponse.from(booking);
    }
}
