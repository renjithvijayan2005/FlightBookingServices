package com.example.booking.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Service
@Slf4j
public class StripePaymentService {

    @Value("${stripe.secret-key:}")
    private String secretKey;

    @Value("${stripe.currency:USD}")
    private String currency;

    public boolean isConfigured() {
        return secretKey != null && !secretKey.isBlank() && !secretKey.equals("sk_test_dummy");
    }

    /**
     * Creates a Stripe PaymentIntent and returns its client_secret for frontend confirmation.
     * Amount is in the smallest currency unit (cents for USD).
     */
    public Map<String, String> createPaymentIntent(BigDecimal amount, String bookingRef) throws StripeException {
        Stripe.apiKey = secretKey;
        long amountCents = amount.multiply(BigDecimal.valueOf(100)).longValue();

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountCents)
                .setCurrency(currency.toLowerCase())
                .setDescription("Flight booking: " + bookingRef)
                .putMetadata("booking_reference", bookingRef)
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .build())
                .build();

        PaymentIntent intent = PaymentIntent.create(params);
        log.info("Stripe PaymentIntent created: {} for booking {}", intent.getId(), bookingRef);

        return Map.of(
                "clientSecret", intent.getClientSecret(),
                "paymentIntentId", intent.getId()
        );
    }

    /**
     * Verifies that a PaymentIntent has status 'succeeded'.
     */
    public boolean verifyPayment(String paymentIntentId) {
        try {
            Stripe.apiKey = secretKey;
            PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId);
            return "succeeded".equals(intent.getStatus());
        } catch (StripeException e) {
            log.error("Stripe verification failed for {}: {}", paymentIntentId, e.getMessage());
            return false;
        }
    }
}
