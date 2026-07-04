package com.example.booking.dto;

import com.example.booking.dto.amadeus.AmadeusFlightOffer;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
public class BookingRequest {

    @NotNull(message = "Source is required")
    private String source;       // "LOCAL" | "AMADEUS"

    private Long flightId;       // required when source = LOCAL

    private AmadeusFlightOffer amadeusOffer;  // required when source = AMADEUS

    @NotEmpty(message = "At least one passenger required")
    private List<PassengerInfo> passengers;

    // Contact details
    @NotBlank(message = "Contact name is required")
    private String contactName;

    @NotBlank(message = "Contact email is required")
    @Email(message = "Invalid email address")
    private String contactEmail;

    private String contactPhone;

    @NotNull(message = "GDPR consent is required")
    private Boolean gdprConsent;

    @Data
    @NoArgsConstructor
    public static class PassengerInfo {
        @NotNull private String firstName;
        @NotNull private String lastName;
        private LocalDate dateOfBirth;
        private String passportNumber;
        private String nationality;
        private String passengerType;  // ADULT | CHILD | INFANT
        private String gender;         // MALE | FEMALE
    }
}
