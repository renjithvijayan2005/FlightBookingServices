package com.example.booking.repository;

import com.example.booking.model.Flight;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FlightRepository extends JpaRepository<Flight, Long> {

    @EntityGraph(attributePaths = "airline")
    List<Flight> findByOriginAirport_AirportCodeAndDestinationAirport_AirportCodeAndDepartureDateTimeBetween(
            String originCode, String destinationCode,
            LocalDateTime start, LocalDateTime end);

    @Query("SELECT f FROM Flight f WHERE f.availableSeats > 0 ORDER BY f.departureDateTime ASC")
    List<Flight> findAvailableFlights();

    @Query("SELECT f FROM Flight f WHERE f.flightNumber = :flightNumber")
    java.util.Optional<Flight> findByFlightNumber(@Param("flightNumber") String flightNumber);
}
