package com.example.booking.service;

import com.example.booking.model.Booking;
import com.example.booking.model.BookingLuggage;
import com.example.booking.model.Payment;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.mail.from:noreply@souloftrips.com}")
    private String fromAddress;

    @Value("${app.mail.from-name:Soul of Trips}")
    private String fromName;

    @Value("${app.notifications.email.enabled:true}")
    private boolean emailEnabled;

    @Async
    public void sendBookingConfirmation(Booking booking, List<BookingLuggage> luggageItems) {
        if (!emailEnabled || booking.getContactEmail() == null) return;
        try {
            Context ctx = new Context();
            ctx.setVariable("booking", booking);
            ctx.setVariable("luggageItems", luggageItems);
            ctx.setVariable("confirmationKey", booking.getConfirmationKey());
            String html = templateEngine.process("email/booking-confirmation", ctx);
            send(booking.getContactEmail(),
                 "Booking Confirmed — " + booking.getBookingReference(), html);
        } catch (Exception e) {
            log.error("Failed to send booking confirmation email to {}: {}",
                      booking.getContactEmail(), e.getMessage());
        }
    }

    @Async
    public void sendPaymentConfirmation(Booking booking, Payment payment) {
        if (!emailEnabled || booking.getContactEmail() == null) return;
        try {
            Context ctx = new Context();
            ctx.setVariable("booking", booking);
            ctx.setVariable("payment", payment);
            ctx.setVariable("confirmationKey", booking.getConfirmationKey());
            String html = templateEngine.process("email/payment-confirmation", ctx);
            send(booking.getContactEmail(),
                 "Payment Receipt — " + booking.getBookingReference(), html);
        } catch (Exception e) {
            log.error("Failed to send payment confirmation email to {}: {}",
                      booking.getContactEmail(), e.getMessage());
        }
    }

    @Async
    public void sendModificationConfirmation(Booking booking) {
        if (!emailEnabled || booking.getContactEmail() == null) return;
        try {
            Context ctx = new Context();
            ctx.setVariable("booking", booking);
            String html = templateEngine.process("email/booking-modification", ctx);
            send(booking.getContactEmail(),
                 "Booking Updated — " + booking.getBookingReference(), html);
        } catch (Exception e) {
            log.error("Failed to send modification email to {}: {}",
                      booking.getContactEmail(), e.getMessage());
        }
    }

    @Async
    public void sendCancellationConfirmation(Booking booking) {
        if (!emailEnabled || booking.getContactEmail() == null) return;
        try {
            Context ctx = new Context();
            ctx.setVariable("booking", booking);
            String html = templateEngine.process("email/booking-cancellation", ctx);
            send(booking.getContactEmail(),
                 "Booking Cancelled — " + booking.getBookingReference(), html);
        } catch (Exception e) {
            log.error("Failed to send cancellation email to {}: {}",
                      booking.getContactEmail(), e.getMessage());
        }
    }

    private void send(String to, String subject, String htmlBody) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromAddress, fromName);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlBody, true);
        mailSender.send(message);
        log.info("Email sent to {} — subject: {}", to, subject);
    }
}
