package com.example.booking.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "airlines")
public class Airline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** IATA 2-letter code e.g. EK, BA, SQ */
    @Column(unique = true, length = 3)
    private String airlineCode;

    @Column(nullable = false)
    private String airlineName;

    private String contactNumber;
    private String operatingRegion;
}
