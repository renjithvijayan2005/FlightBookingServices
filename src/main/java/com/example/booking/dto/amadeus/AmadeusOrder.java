package com.example.booking.dto.amadeus;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AmadeusOrder {
    private String id;
    private String type;
    private List<AmadeusFlightOffer> flightOffers;
    private List<AmadeusPassenger> travelers;
    private AmadeusAssociatedRecords associatedRecords;
}
