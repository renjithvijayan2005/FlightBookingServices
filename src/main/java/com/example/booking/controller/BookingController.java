package com.example.booking.controller;

import com.example.booking.dto.*;
import com.example.booking.model.Booking;
import com.example.booking.model.Payment;
import com.example.booking.service.BookingService;
import com.example.booking.service.StripePaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService       bookingService;
    private final StripePaymentService stripePaymentService;

    @PostMapping
    public ResponseEntity<?> createBooking(@Valid @RequestBody BookingRequest request) {
        try {
            Booking booking = bookingService.createBooking(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(BookingResponse.from(booking, false));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", errorMsg(ex)));
        }
    }

    private static String errorMsg(Exception ex) {
        return ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
    }

    @PostMapping("/{id}/payment")
    public ResponseEntity<?> processPayment(@PathVariable Long id,
                                            @Valid @RequestBody PaymentRequest request) {
        try {
            Payment payment = bookingService.processPayment(id, request);
            Booking booking = payment.getBooking();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "confirmationKey", booking.getConfirmationKey() != null ? booking.getConfirmationKey() : "",
                    "bookingReference", booking.getBookingReference()
            ));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", errorMsg(ex)));
        }
    }

    /** Creates a Stripe PaymentIntent — returns clientSecret for frontend confirmation. */
    @PostMapping("/{id}/stripe-intent")
    public ResponseEntity<?> createStripeIntent(@PathVariable Long id) {
        try {
            if (!stripePaymentService.isConfigured()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Stripe is not configured on this server"));
            }
            Map<String, String> result = bookingService.createStripeIntent(id);
            return ResponseEntity.ok(result);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", errorMsg(ex)));
        }
    }

    /** Look up a booking by reference. Pass confirmationKey to see full details. */
    @GetMapping("/{reference}")
    public ResponseEntity<?> getByReference(@PathVariable String reference,
                                            @RequestParam(required = false) String key) {
        try {
            BookingResponse response = bookingService.getByReference(reference, key);
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", errorMsg(ex)));
        }
    }

    /** Update contact details on an existing booking (requires confirmationKey). */
    @PutMapping("/manage")
    public ResponseEntity<?> modifyBooking(@Valid @RequestBody BookingModifyRequest request) {
        try {
            BookingResponse response = bookingService.modifyBooking(request);
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", errorMsg(ex)));
        }
    }

    /** Cancel a booking (requires confirmationKey). */
    @PostMapping("/{reference}/cancel")
    public ResponseEntity<?> cancelBooking(@PathVariable String reference,
                                           @RequestParam String key) {
        try {
            BookingResponse response = bookingService.cancelBooking(reference, key);
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", errorMsg(ex)));
        }
    }
}
