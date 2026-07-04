package com.example.booking.dto.amadeus;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AmadeusDocument {
    private String documentType;
    private String number;
    private String expiryDate;
    private String issuanceCountry;
    private String nationality;
    private boolean holder;
}
