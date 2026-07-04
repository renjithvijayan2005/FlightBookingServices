package com.example.booking.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 20)
    private String bookingReference;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "flight_id")
    private Flight flight;

    @Column(length = 100)
    private String amadeusOrderId;

    private Integer numPassengers = 1;

    @Column(nullable = false)
    private LocalDateTime bookingDate;

    /** PENDING | CONFIRMED | CANCELLED | FAILED */
    @Column(nullable = false, length = 20)
    private String status;

    /** PENDING | PAID | FAILED | REFUNDED */
    @Column(nullable = false, length = 20)
    private String paymentStatus;

    @Column(precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(precision = 10, scale = 2)
    private BigDecimal luggageTotal = BigDecimal.ZERO;

    // Contact details collected at booking time
    @Column(length = 255)
    private String contactName;

    @Column(length = 255)
    private String contactEmail;

    @Column(length = 50)
    private String contactPhone;

    // GDPR consent
    @Column(nullable = false)
    private boolean gdprConsent = false;

    private LocalDateTime gdprConsentTimestamp;

    /** Shared with user after payment — required to modify/cancel booking */
    @JsonIgnore
    @Column(unique = true, length = 50)
    private String confirmationKey;

    @Column(length = 100)
    private String stripePaymentIntentId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = updatedAt = LocalDateTime.now();
        if (luggageTotal == null) luggageTotal = BigDecimal.ZERO;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
