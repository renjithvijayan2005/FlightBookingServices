package com.example.booking.service;

import com.example.booking.integration.AmadeusApiService;
import com.example.booking.model.Airport;
import com.example.booking.repository.AirportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.interceptor.SimpleKey;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class AirportService {

    private static final int AMADEUS_PAGE_LIMIT = 100;
    private static final int MAX_PREFIX_DEPTH    = 3;
    private static final int RATE_LIMIT_MS       = 150; // 150 ms between calls ≈ 6.7 req/s
    private static final int BATCH_SAVE_SIZE     = 200;

    private static final List<String> ALPHABET = IntStream.rangeClosed('A', 'Z')
            .mapToObj(ch -> String.valueOf((char) ch))
            .toList();

    private final AirportRepository   airportRepository;
    private final AmadeusApiService   amadeusApiService;
    private final CacheManager        cacheManager;

    // ─── Sync progress (volatile — single writer, multiple readers) ───────────

    private final AtomicBoolean running        = new AtomicBoolean(false);
    private final AtomicInteger apiCallsMade   = new AtomicInteger(0);
    private final AtomicInteger airportsFound  = new AtomicInteger(0);
    private final AtomicInteger prefixesDone   = new AtomicInteger(0);
    private volatile int        prefixesTotal  = 0;
    private volatile String     currentPrefix  = "";
    private volatile String     syncError      = null;
    private volatile Instant    startedAt      = null;
    private volatile Instant    completedAt    = null;
    private volatile int        lastSyncCount  = 0;

    // ─── Read queries ─────────────────────────────────────────────────────────

    @Cacheable("airports")
    public List<Airport> getAllAirports() {
        return airportRepository.findAll();
    }

    public Airport getByCode(String code) {
        return airportRepository.findById(code)
                .orElseThrow(() -> new RuntimeException("Airport not found: " + code));
    }

    @Cacheable(value = "airports", key = "'search_' + #query")
    public List<Airport> search(String query) {
        return airportRepository
                .findByAirportNameContainingIgnoreCaseOrCityContainingIgnoreCase(query, query)
                .stream()
                .toList();
    }

    // ─── Blocking sync (used by scheduler or admin POST that wants to wait) ───

    public AirportSyncResult syncAirportsFromAmadeus() {
        return runSync();
    }

    // ─── Async (non-blocking) sync triggered by the controller ───────────────

    @Async("syncExecutor")
    public CompletableFuture<AirportSyncResult> syncAirportsFromAmadeusAsync() {
        return CompletableFuture.completedFuture(runSync());
    }

    // ─── Progress query ───────────────────────────────────────────────────────

    public SyncStatus getSyncStatus() {
        return new SyncStatus(
                running.get(),
                apiCallsMade.get(),
                airportsFound.get(),
                prefixesDone.get(),
                prefixesTotal,
                currentPrefix,
                syncError,
                startedAt,
                completedAt,
                lastSyncCount
        );
    }

    // ─── Core sync logic ──────────────────────────────────────────────────────

    private AirportSyncResult runSync() {
        if (!running.compareAndSet(false, true)) {
            log.warn("Airport sync already running — ignoring duplicate request");
            return new AirportSyncResult(0, List.of(), "Sync already in progress");
        }

        resetCounters();
        Map<String, Airport> airportsByCode  = new LinkedHashMap<>();
        List<String>         truncated        = new ArrayList<>();
        List<String>         errors           = new ArrayList<>();

        prefixesTotal = ALPHABET.size();
        log.info("Starting world airport sync (depth {}, {} root prefixes, {}ms rate limit)",
                MAX_PREFIX_DEPTH, ALPHABET.size(), RATE_LIMIT_MS);

        for (String prefix : ALPHABET) {
            currentPrefix = prefix;
            try {
                collectAirports(prefix, 1, airportsByCode, truncated);
                flushBatch(airportsByCode, false);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                syncError = "Sync interrupted at prefix " + prefix;
                log.warn("Airport sync interrupted at prefix '{}'", prefix);
                break;
            } catch (Exception ex) {
                String msg = "Error at prefix '" + prefix + "': " + ex.getMessage();
                errors.add(msg);
                log.error(msg, ex);
            }
            prefixesDone.incrementAndGet();
        }

        // Final flush
        try {
            flushBatch(airportsByCode, true);
        } catch (Exception ex) {
            log.error("Error during final batch save", ex);
        }

        refreshAirportCache();

        int total = airportsFound.get();
        lastSyncCount = total;
        completedAt = Instant.now();
        running.set(false);
        currentPrefix = "";

        log.info("Airport sync complete: {} airports saved, {} API calls, {} truncated prefixes",
                total, apiCallsMade.get(), truncated.size());

        return new AirportSyncResult(total, truncated, errors.isEmpty() ? null : String.join("; ", errors));
    }

    /**
     * Recursive prefix descent.
     *
     * Strategy:
     *  - depth 1 (A–Z): fetch first page; if full (100 results) go deeper.
     *  - depth 2 (AA–ZZ): same rule.
     *  - depth 3 (AAA–ZZZ): paginate through all pages, no further descent.
     */
    private void collectAirports(
            String keyword,
            int depth,
            Map<String, Airport> airportsByCode,
            List<String> truncatedPrefixes) throws InterruptedException {

        rateLimitSleep();
        List<Airport> firstPage = safeSearch(keyword, 0, AMADEUS_PAGE_LIMIT);

        if (firstPage.isEmpty()) {
            return;
        }

        // If page is full AND we can go deeper, drill down instead of paginating
        if (depth < MAX_PREFIX_DEPTH && firstPage.size() == AMADEUS_PAGE_LIMIT) {
            for (String letter : ALPHABET) {
                collectAirports(keyword + letter, depth + 1, airportsByCode, truncatedPrefixes);
            }
            return;
        }

        // At max depth (or a non-full first page): collect + paginate
        appendAirports(firstPage, airportsByCode);
        airportsFound.set(airportsByCode.size());
        int offset = firstPage.size();

        List<Airport> currentPage = firstPage;
        while (currentPage.size() == AMADEUS_PAGE_LIMIT) {
            rateLimitSleep();
            List<Airport> nextPage = safeSearch(keyword, offset, AMADEUS_PAGE_LIMIT);
            if (nextPage.isEmpty()) {
                break;
            }
            appendAirports(nextPage, airportsByCode);
            airportsFound.set(airportsByCode.size());
            offset += nextPage.size();
            currentPage = nextPage;

            // Safety valve: stop paginating if we've gone very deep
            if (offset >= AMADEUS_PAGE_LIMIT * 10) {
                truncatedPrefixes.add(keyword);
                log.warn("Prefix '{}' has 1000+ airports — stopping pagination to avoid quota exhaustion", keyword);
                break;
            }
        }
    }

    private List<Airport> safeSearch(String keyword, int offset, int limit) {
        try {
            apiCallsMade.incrementAndGet();
            return amadeusApiService.searchAirports(keyword, offset, limit);
        } catch (Exception ex) {
            log.warn("Amadeus search failed for keyword='{}' offset={}: {}", keyword, offset, ex.getMessage());
            return List.of();
        }
    }

    private void appendAirports(List<Airport> airports, Map<String, Airport> byCode) {
        for (Airport a : airports) {
            if (a != null && a.getAirportCode() != null) {
                byCode.put(a.getAirportCode(), a);
            }
        }
    }

    private void flushBatch(Map<String, Airport> byCode, boolean force) {
        if (force || byCode.size() >= BATCH_SAVE_SIZE) {
            if (!byCode.isEmpty()) {
                airportRepository.saveAll(new ArrayList<>(byCode.values()));
                log.debug("Flushed {} airports to DB", byCode.size());
            }
        }
    }

    private void rateLimitSleep() throws InterruptedException {
        Thread.sleep(RATE_LIMIT_MS);
    }

    private void resetCounters() {
        apiCallsMade.set(0);
        airportsFound.set(0);
        prefixesDone.set(0);
        prefixesTotal = 0;
        currentPrefix = "";
        syncError = null;
        startedAt = Instant.now();
        completedAt = null;
    }

    private void refreshAirportCache() {
        Cache cache = cacheManager.getCache("airports");
        if (cache == null) {
            return;
        }
        cache.clear();
        cache.put(SimpleKey.EMPTY, airportRepository.findAll());
    }

    // ─── Result / Status records ──────────────────────────────────────────────

    public record AirportSyncResult(
            int airportCount,
            List<String> truncatedPrefixes,
            String errorMessage
    ) {}

    public record SyncStatus(
            boolean running,
            int apiCallsMade,
            int airportsFound,
            int prefixesDone,
            int prefixesTotal,
            String currentPrefix,
            String errorMessage,
            Instant startedAt,
            Instant completedAt,
            int lastSyncCount
    ) {}
}
