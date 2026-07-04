package com.example.booking.controller;

import com.example.booking.model.Airport;
import com.example.booking.service.AirportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/airports")
@RequiredArgsConstructor
public class AirportController {

    private final AirportService airportService;

    @GetMapping
    public ResponseEntity<List<Airport>> getAllAirports() {
        return ResponseEntity.ok(airportService.getAllAirports());
    }

    @GetMapping("/{code}")
    public ResponseEntity<Airport> getAirport(@PathVariable String code) {
        try {
            return ResponseEntity.ok(airportService.getByCode(code));
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<Airport>> search(@RequestParam String q) {
        return ResponseEntity.ok(airportService.search(q));
    }

    /**
     * Start a background Amadeus sync (non-blocking).
     * Returns 409 if a sync is already running.
     */
    @PostMapping("/sync/start")
    public ResponseEntity<Map<String, Object>> startSync() {
        AirportService.SyncStatus status = airportService.getSyncStatus();
        if (status.running()) {
            return ResponseEntity.status(409).body(Map.of(
                    "message", "Sync already running",
                    "status", buildStatusMap(status)
            ));
        }
        airportService.syncAirportsFromAmadeusAsync();
        return ResponseEntity.accepted().body(Map.of(
                "message", "Amadeus airport sync started in background",
                "status", buildStatusMap(airportService.getSyncStatus())
        ));
    }

    /**
     * Poll sync progress.
     */
    @GetMapping("/sync/status")
    public ResponseEntity<Map<String, Object>> syncStatus() {
        return ResponseEntity.ok(buildStatusMap(airportService.getSyncStatus()));
    }

    /**
     * Blocking sync (kept for backward-compat / scheduler use).
     */
    @PostMapping("/sync")
    public ResponseEntity<Map<String, Object>> syncAirportsBlocking() {
        AirportService.AirportSyncResult result = airportService.syncAirportsFromAmadeus();
        return ResponseEntity.ok(Map.of(
                "airportCount",       result.airportCount(),
                "truncatedPrefixes",  result.truncatedPrefixes(),
                "errorMessage",       result.errorMessage() == null ? "" : result.errorMessage()
        ));
    }

    private Map<String, Object> buildStatusMap(AirportService.SyncStatus s) {
        int total = s.prefixesTotal();
        int done  = s.prefixesDone();
        int pct   = total == 0 ? 0 : (int) ((done * 100.0) / total);
        return Map.ofEntries(
            Map.entry("running", s.running()),
            Map.entry("apiCallsMade", s.apiCallsMade()),
            Map.entry("airportsFound", s.airportsFound()),
            Map.entry("prefixesDone", done),
            Map.entry("prefixesTotal", total),
            Map.entry("progressPct", pct),
            Map.entry("currentPrefix", s.currentPrefix() == null ? "" : s.currentPrefix()),
            Map.entry("errorMessage", s.errorMessage() == null ? "" : s.errorMessage()),
            Map.entry("lastSyncCount", s.lastSyncCount()),
            Map.entry("startedAt", s.startedAt() == null ? "" : s.startedAt().toString()),
            Map.entry("completedAt", s.completedAt() == null ? "" : s.completedAt().toString())
        );
    }
}
