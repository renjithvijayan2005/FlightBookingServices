package com.example.booking.controller;

import com.example.booking.model.Flight;
import com.example.booking.service.FlightService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * GAP FIXES vs original:
 *  - /search now accepts LocalDate (not LocalDateTime) matching Amadeus format
 *  - Added passengers, cabin, nonStop params
 *  - Returns FlightSearchResult (union of local + Amadeus) not raw Flight entities
 *  - Proper error handling with ResponseEntity
 */
@RestController
@RequestMapping("/api/flights")
@RequiredArgsConstructor
public class FlightController {

    private final FlightService flightService;

    @GetMapping("/search")
    public ResponseEntity<List<FlightService.FlightSearchResult>> searchFlights(
            @RequestParam String origin,
            @RequestParam String destination,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate departureDate,
            @RequestParam(defaultValue = "1")  int adults,
            @RequestParam(defaultValue = "0")  int children,
            @RequestParam(defaultValue = "ECONOMY") String cabinClass,
            @RequestParam(defaultValue = "false")   boolean nonStop) {

        List<FlightService.FlightSearchResult> results = flightService.searchFlights(
                origin, destination, departureDate, adults, children, cabinClass, nonStop);
        return ResponseEntity.ok(results);
    }

    @GetMapping
    public ResponseEntity<List<Flight>> getAllFlights() {
        return ResponseEntity.ok(flightService.getAllFlights());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Flight> getFlight(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(flightService.getFlightById(id));
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }
}
