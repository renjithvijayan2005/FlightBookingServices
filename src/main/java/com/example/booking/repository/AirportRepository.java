package com.example.booking.repository;

import com.example.booking.model.Airport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AirportRepository extends JpaRepository<Airport, String> {
    List<Airport> findByAirportNameContainingIgnoreCaseOrCityContainingIgnoreCase(String name, String city);
}