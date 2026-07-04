package com.example.booking.dto;

import com.example.booking.model.Booking;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Safe booking response — exposes confirmationKey only at creation time.
 */
@Data
public class BookingResponse {

    private Long id;
    private String bookingReference;
    private String confirmationKey;   // shown once after booking/payment
    private String status;
    private String paymentStatus;
    private BigDecimal totalAmount;
    private BigDecimal luggageTotal;
    private String contactName;
    private String contactEmail;
    private String contactPhone;
    private LocalDateTime bookingDate;
    private LocalDateTime createdAt;

    // Flight summary (populated when flight is a LOCAL flight)
    private String flightNumber;
    private String originCode;
    private String destinationCode;
    private LocalDateTime departureDateTime;
    private LocalDateTime arrivalDateTime;

    public static BookingResponse from(Booking b, boolean includeKey) {
        BookingResponse r = new BookingResponse();
        r.id               = b.getId();
        r.bookingReference = b.getBookingReference();
        r.confirmationKey  = includeKey ? b.getConfirmationKey() : null;
        r.status           = b.getStatus();
        r.paymentStatus    = b.getPaymentStatus();
        r.totalAmount      = b.getTotalAmount();
        r.luggageTotal     = b.getLuggageTotal();
        r.contactName      = b.getContactName();
        r.contactEmail     = b.getContactEmail();
        r.contactPhone     = b.getContactPhone();
        r.bookingDate      = b.getBookingDate();
        r.createdAt        = b.getCreatedAt();
        if (b.getFlight() != null) {
            r.flightNumber     = b.getFlight().getFlightNumber();
            r.originCode       = b.getFlight().getOriginAirport() != null
                                 ? b.getFlight().getOriginAirport().getAirportCode() : null;
            r.destinationCode  = b.getFlight().getDestinationAirport() != null
                                 ? b.getFlight().getDestinationAirport().getAirportCode() : null;
            r.departureDateTime = b.getFlight().getDepartureDateTime();
            r.arrivalDateTime   = b.getFlight().getArrivalDateTime();
        }
        return r;
    }
}
