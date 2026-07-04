package com.example.booking.integration;

import com.example.booking.dto.amadeus.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class AmadeusApiService {

    private final RestTemplate restTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${amadeus.api.token-url}")
    private String tokenUrl;

    @Value("${amadeus.api.flight-search-url}")
    private String flightSearchUrl;

    @Value("${amadeus.api.locations-url}")
    private String locationsUrl;

    @Value("${amadeus.api.flight-price-url}")
    private String flightPriceUrl;

    @Value("${amadeus.api.booking-url}")
    private String bookingUrl;

    @Value("${amadeus.api.client-id}")
    private String clientId;

    @Value("${amadeus.api.client-secret}")
    private String clientSecret;

    private static final String TOKEN_CACHE_KEY = "amadeus:access_token";
    private volatile String inMemoryAccessToken;
    private volatile Instant inMemoryAccessTokenExpiresAt;

    // ─── Token Management ───────────────────────────────────────────────────

    public String getAccessToken() {
        if (hasValidInMemoryToken()) {
            return inMemoryAccessToken;
        }

        try {
            Object cached = redisTemplate.opsForValue().get(TOKEN_CACHE_KEY);
            if (cached != null) {
                String token = cached.toString();
                cacheTokenInMemory(token, 300);
                return token;
            }
        } catch (DataAccessException ex) {
            log.warn("Redis unavailable while reading Amadeus token cache, falling back to direct auth: {}", ex.getMessage());
        }

        log.info("Fetching new Amadeus access token");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);

        ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                tokenUrl, new HttpEntity<>(body, headers), JsonNode.class);

        JsonNode tokenData = response.getBody();
        if (tokenData == null || !tokenData.has("access_token")) {
            throw new RuntimeException("Failed to obtain Amadeus access token");
        }

        String token = tokenData.get("access_token").asText();
        int expiresIn = tokenData.path("expires_in").asInt(1799);

        cacheTokenInMemory(token, expiresIn);

        try {
            redisTemplate.opsForValue().set(TOKEN_CACHE_KEY, token, Duration.ofSeconds(Math.max(60, expiresIn - 60)));
        } catch (DataAccessException ex) {
            log.warn("Redis unavailable while writing Amadeus token cache, using in-memory fallback only: {}", ex.getMessage());
        }

        return token;
    }

    // ─── Flight Search ───────────────────────────────────────────────────────

    @CircuitBreaker(name = "amadeus", fallbackMethod = "searchFlightsFallback")
    @Retry(name = "amadeus")
    @Cacheable(value = "flightOffers",
               key = "#origin + '-' + #destination + '-' + #departureDate + '-' + #adults")
    public AmadeusFlightSearchResponse searchFlights(
            String origin,
            String destination,
            String departureDate,
            int adults,
            String cabinClass,
            boolean nonStop) {

        HttpHeaders headers = authHeaders();
        String url = flightSearchUrl
                + "?originLocationCode=" + origin
                + "&destinationLocationCode=" + destination
                + "&departureDate=" + departureDate
                + "&adults=" + adults
                + "&travelClass=" + cabinClass
                + "&nonStop=" + nonStop
                + "&currencyCode=USD"
                + "&max=20";

        ResponseEntity<AmadeusFlightSearchResponse> response =
                restTemplate.exchange(url, HttpMethod.GET,
                        new HttpEntity<>(headers), AmadeusFlightSearchResponse.class);

        int count = (response.getBody() != null && response.getBody().getData() != null)
                ? response.getBody().getData().size() : 0;
        log.info("Amadeus search returned {} offers for {}->{} on {}", count, origin, destination, departureDate);

        return response.getBody();
    }

    public List<com.example.booking.model.Airport> searchAirports(String keyword, int offset, int limit) {
        HttpHeaders headers = authHeaders();
        String url = UriComponentsBuilder.fromHttpUrl(locationsUrl)
                .queryParam("subType", "AIRPORT")
                .queryParam("keyword", keyword)
                .queryParam("view", "FULL")
                .queryParam("page[limit]", limit)
                .queryParam("page[offset]", offset)
                .build(true)
                .toUriString();

        ResponseEntity<JsonNode> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                JsonNode.class);

        JsonNode data = response.getBody() == null ? null : response.getBody().path("data");
        if (data == null || !data.isArray()) {
            return List.of();
        }

        List<com.example.booking.model.Airport> airports = new ArrayList<>();
        for (JsonNode node : data) {
            String iataCode = textValue(node, "iataCode");
            if (iataCode == null || iataCode.length() != 3) {
                continue;
            }

            JsonNode address = node.path("address");
            String city = textValue(address, "cityName");
            String country = textValue(address, "countryName");
            String detailedName = textValue(node, "detailedName");

            airports.add(new com.example.booking.model.Airport(
                    iataCode.toUpperCase(Locale.ROOT),
                    defaultIfBlank(textValue(node, "name"), iataCode.toUpperCase(Locale.ROOT)),
                    city,
                    country,
                    detailedName,
                    null,
                    defaultIfBlank(textValue(node, "timeZoneOffset"), "UTC")
            ));
        }

        return airports;
    }

    public AmadeusFlightSearchResponse searchFlightsFallback(
            String origin, String destination, String departureDate,
            int adults, String cabinClass, boolean nonStop, Throwable ex) {
        log.error("Amadeus circuit open, returning empty. Error: {}", ex.getMessage());
        return new AmadeusFlightSearchResponse(List.of());
    }

    // ─── Price Confirmation ───────────────────────────────────────────────────

    @CircuitBreaker(name = "amadeus")
    @Retry(name = "amadeus")
    public AmadeusFlightOffer confirmPrice(AmadeusFlightOffer offer) {
        HttpHeaders headers = authHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        AmadeusPriceRequest priceRequest = new AmadeusPriceRequest(offer);
        HttpEntity<AmadeusPriceRequest> request = new HttpEntity<>(priceRequest, headers);

        ResponseEntity<JsonNode> response =
                restTemplate.postForEntity(flightPriceUrl, request, JsonNode.class);

        try {
            JsonNode data = response.getBody()
                    .path("data").path("flightOffers").get(0);
            return objectMapper.treeToValue(data, AmadeusFlightOffer.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse price confirmation response", e);
        }
    }

    // ─── Flight Order (Booking) ───────────────────────────────────────────────

    @CircuitBreaker(name = "amadeus")
    public AmadeusOrderResponse createOrder(AmadeusFlightOffer confirmedOffer,
                                            List<AmadeusPassenger> passengers) {
        HttpHeaders headers = authHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        AmadeusOrderRequest orderRequest = new AmadeusOrderRequest(confirmedOffer, passengers);
        HttpEntity<AmadeusOrderRequest> request = new HttpEntity<>(orderRequest, headers);

        ResponseEntity<AmadeusOrderResponse> response =
                restTemplate.postForEntity(bookingUrl, request, AmadeusOrderResponse.class);

        String orderId = (response.getBody() != null && response.getBody().getData() != null)
                ? response.getBody().getData().getId() : "null";
        log.info("Amadeus order created: {}", orderId);

        return response.getBody();
    }

    // ─── Helper ──────────────────────────────────────────────────────────────

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(getAccessToken());
        return headers;
    }

    private boolean hasValidInMemoryToken() {
        return inMemoryAccessToken != null
                && inMemoryAccessTokenExpiresAt != null
                && Instant.now().isBefore(inMemoryAccessTokenExpiresAt);
    }

    private void cacheTokenInMemory(String token, int expiresInSeconds) {
        inMemoryAccessToken = token;
        inMemoryAccessTokenExpiresAt = Instant.now().plusSeconds(Math.max(60, expiresInSeconds - 60L));
    }

    private String textValue(JsonNode node, String fieldName) {
        if (node == null) {
            return null;
        }

        JsonNode value = node.get(fieldName);
        if (value == null || value.isNull()) {
            return null;
        }

        String text = value.asText();
        return text == null || text.isBlank() ? null : text;
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
