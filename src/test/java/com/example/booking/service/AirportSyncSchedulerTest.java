package com.example.booking.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AirportSyncSchedulerTest {

    @Mock
    private AirportService airportService;

    @Test
    void refreshAirportsFromAmadeusTriggersSync() {
        when(airportService.syncAirportsFromAmadeus())
            .thenReturn(new AirportService.AirportSyncResult(42, List.of(), null));

        AirportSyncScheduler scheduler = new AirportSyncScheduler(airportService);
        scheduler.refreshAirportsFromAmadeus();

        verify(airportService).syncAirportsFromAmadeus();
    }
}