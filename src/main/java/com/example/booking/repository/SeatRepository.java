package com.example.booking.repository;

import com.example.booking.model.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findByFlight_Id(Long flightId);

    Optional<Seat> findByFlight_IdAndSeatNumber(Long flightId, String seatNumber);

    boolean existsByFlight_IdAndSeatNumber(Long flightId, String seatNumber);

    @Query("SELECT s.seatNumber FROM Seat s WHERE s.flight.id = :flightId")
    List<String> findOccupiedSeatNumbers(@Param("flightId") Long flightId);

    List<Seat> findByBooking_Id(Long bookingId);
}
