package com.example.booking.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * GAP FIXES:
 *  - price changed from Double to BigDecimal (financial precision)
 *  - Added cabinClass, totalSeats, amadeusOfferId, source fields
 *  - Added airline.airlineCode used for Amadeus mapping
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "flights", indexes = {
    @Index(name = "idx_flights_route_date",
           columnList = "origin_airport_code, destination_airport_code, departure_date_time")
})
public class Flight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 20)
    private String flightNumber;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "airline_id", nullable = false)
    private Airline airline;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "origin_airport_code", nullable = false)
    private Airport originAirport;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "destination_airport_code", nullable = false)
    private Airport destinationAirport;

    @Column(nullable = false)
    private LocalDateTime departureDateTime;

    @Column(nullable = false)
    private LocalDateTime arrivalDateTime;

    @Column(nullable = false)
    private Integer availableSeats;

    @Column(nullable = false)
    private Integer totalSeats;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(length = 20)
    private String cabinClass = "ECONOMY";

    /** Amadeus offer ID — used to re-price before booking */
    @Column(length = 500)
    private String amadeusOfferId;

    /** MANUAL | AMADEUS */
    @Column(length = 20)
    private String source = "MANUAL";

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
