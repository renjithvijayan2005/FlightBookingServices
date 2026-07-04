package com.example.booking.dto.amadeus;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AmadeusPriceRequest {

    private Data data;

    public AmadeusPriceRequest(AmadeusFlightOffer offer) {
        this.data = new Data("flight-offers-pricing", List.of(offer));
    }

    @lombok.Data
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Data {
        private String type;
        private List<AmadeusFlightOffer> flightOffers;
    }
}
