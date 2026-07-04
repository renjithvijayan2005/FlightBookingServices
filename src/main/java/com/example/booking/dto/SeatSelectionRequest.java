package com.example.booking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class SeatSelectionRequest {

    @NotNull
    private Long bookingId;

    @NotNull
    private Long flightId;

    /** One seat number per passenger, e.g. ["12A", "12B"] */
    @NotNull
    private List<@NotBlank String> seatNumbers;
}
