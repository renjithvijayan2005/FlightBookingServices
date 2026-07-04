package com.example.booking.dto.amadeus;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AmadeusPassenger {
    private String id;
    private String dateOfBirth;
    private String travelerType;
    private AmadeusPassengerName name;
    private List<AmadeusDocument> documents;
    private AmadeusContact contact;
}
