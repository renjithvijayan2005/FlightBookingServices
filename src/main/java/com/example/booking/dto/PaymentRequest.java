package com.example.booking.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PaymentRequest {

    @NotBlank(message = "Payment method is required")
    private String paymentMethod;   // CREDIT_CARD | DEBIT_CARD | STRIPE

    private String cardNumber;
    private String cardExpiry;
    private String cardCvv;
    private String cardHolderName;

    /** Stripe PaymentIntent ID — provided when using Stripe payment flow */
    private String stripePaymentIntentId;
}
