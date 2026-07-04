package com.example.booking.controller;

import com.example.booking.dto.SeatSelectionRequest;
import com.example.booking.model.Seat;
import com.example.booking.service.SeatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class SeatController {

    private final SeatService seatService;

    /** Returns available/occupied seat map for a flight. */
    @GetMapping("/api/flights/{flightId}/seats")
    public ResponseEntity<?> getSeats(@PathVariable Long flightId) {
        try {
            Map<String, Object> seatMap = seatService.getSeatsForFlight(flightId);
            return ResponseEntity.ok(seatMap);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    /** Assign seats to a booking. */
    @PostMapping("/api/bookings/{bookingId}/seats")
    public ResponseEntity<?> selectSeats(@PathVariable Long bookingId,
                                         @Valid @RequestBody SeatSelectionRequest request) {
        request.setBookingId(bookingId);
        try {
            List<Seat> seats = seatService.selectSeats(
                    request.getBookingId(), request.getFlightId(), request.getSeatNumbers());
            return ResponseEntity.ok(seats);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }
}
