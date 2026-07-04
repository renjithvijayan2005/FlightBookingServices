package com.example.booking.service;

import com.example.booking.model.Booking;
import com.example.booking.model.Passenger;
import com.example.booking.model.Payment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Thin facade kept for backwards compatibility with existing callers.
 * Real sending is delegated to EmailService.
 */
@Service
@Slf4j
public class NotificationService {

    private final EmailService emailService;

    public NotificationService(EmailService emailService) {
        this.emailService = emailService;
    }

    public void sendBookingConfirmation(Booking booking, List<Passenger> passengers) {
        log.info("Booking confirmed: ref={}, passengers={}", booking.getBookingReference(), passengers.size());
        emailService.sendBookingConfirmation(booking, List.of());
    }

    public void sendPaymentConfirmation(Booking booking, Payment payment) {
        log.info("Payment confirmed: ref={}, txn={}", booking.getBookingReference(), payment.getTransactionId());
        emailService.sendPaymentConfirmation(booking, payment);
    }

    public void sendCancellationConfirmation(Booking booking) {
        log.info("Booking cancelled: ref={}", booking.getBookingReference());
        emailService.sendCancellationConfirmation(booking);
    }
}
