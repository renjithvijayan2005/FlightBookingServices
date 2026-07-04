package com.example.booking.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "booking_luggage")
public class BookingLuggage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "passenger_id")
    private Passenger passenger;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "luggage_option_id", nullable = false)
    private LuggageOption luggageOption;

    private int quantity = 1;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerUnit = BigDecimal.ZERO;
}
