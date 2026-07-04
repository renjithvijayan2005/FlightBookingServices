package com.example.booking.service;

import com.example.booking.exception.BookingException;
import com.example.booking.model.Booking;
import com.example.booking.model.Flight;
import com.example.booking.model.Seat;
import com.example.booking.repository.BookingRepository;
import com.example.booking.repository.FlightRepository;
import com.example.booking.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class SeatService {

    private static final String[] COLUMNS = {"A", "B", "C", "D", "E", "F"};
    private static final int BUSINESS_ROWS = 4;

    private final SeatRepository    seatRepository;
    private final FlightRepository  flightRepository;
    private final BookingRepository bookingRepository;

    /** Returns all seat numbers for a flight with their availability status. */
    public Map<String, Object> getSeatsForFlight(Long flightId) {
        Flight flight = flightRepository.findById(flightId)
                .orElseThrow(() -> new BookingException("Flight not found: " + flightId));

        int totalRows = Math.max(1, (int) Math.ceil(flight.getTotalSeats() / (double) COLUMNS.length));
        Set<String> occupied = new HashSet<>(seatRepository.findOccupiedSeatNumbers(flightId));

        List<Map<String, Object>> seats = new ArrayList<>();
        for (int row = 1; row <= totalRows; row++) {
            for (String col : COLUMNS) {
                String num = row + col;
                Map<String, Object> seat = new LinkedHashMap<>();
                seat.put("seatNumber", num);
                seat.put("row", row);
                seat.put("column", col);
                seat.put("cabinClass", row <= BUSINESS_ROWS ? "BUSINESS" : "ECONOMY");
                seat.put("available", !occupied.contains(num));
                seats.add(seat);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("flightId", flightId);
        result.put("totalSeats", flight.getTotalSeats());
        result.put("availableSeats", flight.getAvailableSeats());
        result.put("seats", seats);
        result.put("columns", COLUMNS);
        result.put("businessRows", BUSINESS_ROWS);
        return result;
    }

    @Transactional
    public List<Seat> selectSeats(Long bookingId, Long flightId, List<String> seatNumbers) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingException("Booking not found: " + bookingId));
        Flight flight = flightRepository.findById(flightId)
                .orElseThrow(() -> new BookingException("Flight not found: " + flightId));

        // Release previously selected seats for this booking
        List<Seat> existing = seatRepository.findByBooking_Id(bookingId);
        seatRepository.deleteAll(existing);

        List<Seat> selected = new ArrayList<>();
        for (String num : seatNumbers) {
            if (seatRepository.existsByFlight_IdAndSeatNumber(flightId, num)) {
                throw new BookingException("Seat " + num + " is already taken");
            }
            int row = Integer.parseInt(num.replaceAll("[A-F]", ""));
            String cabinClass = row <= BUSINESS_ROWS ? "BUSINESS" : "ECONOMY";

            Seat seat = new Seat();
            seat.setFlight(flight);
            seat.setBooking(booking);
            seat.setSeatNumber(num);
            seat.setCabinClass(cabinClass);
            selected.add(seatRepository.save(seat));
        }
        return selected;
    }
}
