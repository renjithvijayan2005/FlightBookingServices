package com.example.booking.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "airports")
public class Airport {

    @Id
    @Column(length = 3)
    private String airportCode;

    @Column(nullable = false)
    private String airportName;

    private String city;
    private String country;
    private String location;
    private String facilities;

    @Column(length = 50)
    private String timezone;
}
