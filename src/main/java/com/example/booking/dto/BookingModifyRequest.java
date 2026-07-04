package com.example.booking.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class BookingModifyRequest {

    @NotBlank(message = "Booking reference is required")
    private String bookingReference;

    @NotBlank(message = "Confirmation key is required")
    private String confirmationKey;

    // Optional fields — only non-null values are updated
    private String contactName;
    private String contactEmail;
    private String contactPhone;
}
