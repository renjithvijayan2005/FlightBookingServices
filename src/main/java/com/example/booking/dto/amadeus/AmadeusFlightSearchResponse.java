package com.example.booking.dto.amadeus;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AmadeusFlightSearchResponse {
    private List<AmadeusFlightOffer> data;
    private Map<String, Object> meta;
    private Map<String, Object> dictionaries;

    public AmadeusFlightSearchResponse(List<AmadeusFlightOffer> data) {
        this.data = data;
    }
}
