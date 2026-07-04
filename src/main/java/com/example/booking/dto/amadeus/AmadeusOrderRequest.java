package com.example.booking.dto.amadeus;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AmadeusOrderRequest {

    private OrderData data;

    public AmadeusOrderRequest(AmadeusFlightOffer confirmedOffer, List<AmadeusPassenger> travelers) {
        this.data = new OrderData("flight-order", List.of(confirmedOffer), travelers);
    }

    @lombok.Data
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OrderData {
        private String type;
        private List<AmadeusFlightOffer> flightOffers;
        private List<AmadeusPassenger> travelers;
    }
}
