package com.example.booking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AirportSyncScheduler {

    private final AirportService airportService;

    @Scheduled(fixedDelayString = "${app.airports.sync.fixed-delay-ms:3600000}")
    public void refreshAirportsFromAmadeus() {
        AirportService.AirportSyncResult result = airportService.syncAirportsFromAmadeus();
        log.info(
                "Hourly airport sync completed: {} airports, {} truncated prefixes{}",
                result.airportCount(),
                result.truncatedPrefixes().size(),
                result.errorMessage() == null ? "" : " | errors: " + result.errorMessage()
        );
    }
}