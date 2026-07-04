package com.example.booking.controller;

import com.example.booking.dto.LuggageSelectionRequest;
import com.example.booking.model.BookingLuggage;
import com.example.booking.model.LuggageOption;
import com.example.booking.service.LuggageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/luggage")
@RequiredArgsConstructor
public class LuggageController {

    private final LuggageService luggageService;

    /** All available luggage add-on options. */
    @GetMapping("/options")
    public ResponseEntity<List<LuggageOption>> getOptions() {
        return ResponseEntity.ok(luggageService.getAllOptions());
    }

    /** Save/replace the luggage selection for a booking. */
    @PostMapping("/bookings/{bookingId}/select")
    public ResponseEntity<?> selectLuggage(@PathVariable Long bookingId,
                                           @Valid @RequestBody LuggageSelectionRequest request) {
        request.setBookingId(bookingId);
        try {
            List<BookingLuggage> saved = luggageService.saveSelection(request);
            return ResponseEntity.ok(saved);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    /** Get luggage selections for a booking. */
    @GetMapping("/bookings/{bookingId}")
    public ResponseEntity<List<BookingLuggage>> getLuggage(@PathVariable Long bookingId) {
        return ResponseEntity.ok(luggageService.getByBookingId(bookingId));
    }
}
