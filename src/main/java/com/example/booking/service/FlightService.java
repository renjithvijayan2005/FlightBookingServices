package com.example.booking.service;

import com.example.booking.dto.amadeus.*;
import com.example.booking.integration.AmadeusApiService;
import com.example.booking.model.Flight;
import com.example.booking.repository.FlightRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FlightService {

    private final FlightRepository flightRepository;
    private final AmadeusApiService amadeusApiService;

    public List<FlightSearchResult> searchFlights(
            String origin,
            String destination,
            LocalDate departureDate,
            int adults,
            int children,
            String cabinClass,
            boolean nonStop) {

        List<FlightSearchResult> results = new ArrayList<>();

        // ── 1. Local DB flights ──────────────────────────────────────────
        LocalDateTime startOfDay = departureDate.atStartOfDay();
        LocalDateTime endOfDay   = departureDate.atTime(LocalTime.MAX);

        List<Flight> localFlights = flightRepository
                .findByOriginAirport_AirportCodeAndDestinationAirport_AirportCodeAndDepartureDateTimeBetween(
                        origin, destination, startOfDay, endOfDay);

        localFlights.forEach(f -> results.add(FlightSearchResult.fromLocal(f)));
        log.info("Local DB returned {} flights for {}->{} on {}",
                localFlights.size(), origin, destination, departureDate);

        // ── 2. Amadeus live offers ────────────────────────────────────────
        try {
            AmadeusFlightSearchResponse amadeusResponse = amadeusApiService.searchFlights(
                    origin, destination, departureDate.toString(),
                    adults + children, mapCabinClass(cabinClass), nonStop);

            if (amadeusResponse != null && amadeusResponse.getData() != null) {
                amadeusResponse.getData().forEach(offer ->
                        results.add(FlightSearchResult.fromAmadeus(offer)));
                log.info("Amadeus returned {} offers", amadeusResponse.getData().size());
            }
        } catch (Exception ex) {
            log.error("Amadeus search failed (returning local results only): {}", ex.getMessage());
        }

        return results;
    }

    @Cacheable("flights")
    public List<Flight> getAllFlights() {
        return flightRepository.findAll();
    }

    public Flight getFlightById(Long id) {
        return flightRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Flight not found: " + id));
    }

    private String mapCabinClass(String cabinClass) {
        if (cabinClass == null) return "ECONOMY";
        return switch (cabinClass.toUpperCase()) {
            case "BUSINESS"        -> "BUSINESS";
            case "FIRST"           -> "FIRST";
            case "PREMIUM_ECONOMY" -> "PREMIUM_ECONOMY";
            default                -> "ECONOMY";
        };
    }

    // ─── Result DTO (union of local + Amadeus) ─────────────────────────────

    public record FlightSearchResult(
            String id,
            String source,
            String flightNumber,
            String airlineName,
            String airlineCode,
            String originCode,
            String destinationCode,
            String departureDateTime,
            String arrivalDateTime,
            String duration,
            int availableSeats,
            double price,
            String currency,
            String cabinClass,
            String amadeusOfferId,
            AmadeusFlightOffer amadeusRawOffer   // full offer returned to frontend for booking
    ) {
        public static FlightSearchResult fromLocal(Flight f) {
            return new FlightSearchResult(
                    String.valueOf(f.getId()),
                    "LOCAL",
                    f.getFlightNumber(),
                    f.getAirline().getAirlineName(),
                    f.getAirline().getAirlineCode(),
                    f.getOriginAirport().getAirportCode(),
                    f.getDestinationAirport().getAirportCode(),
                    f.getDepartureDateTime().toString(),
                    f.getArrivalDateTime().toString(),
                    null,
                    f.getAvailableSeats(),
                    f.getPrice().doubleValue(),
                    "USD",
                    f.getCabinClass(),
                    null,
                    null
            );
        }

        public static FlightSearchResult fromAmadeus(AmadeusFlightOffer offer) {
            List<AmadeusFlightOffer.Segment> segments =
                    offer.getItineraries().get(0).getSegments();
            AmadeusFlightOffer.Segment first = segments.get(0);
            AmadeusFlightOffer.Segment last  = segments.get(segments.size() - 1);
            AmadeusFlightOffer.Price price   = offer.getPrice();

            // Safely extract cabin class
            String cabin = "ECONOMY";
            List<AmadeusFlightOffer.TravelerPricing> pricings = offer.getTravelerPricings();
            if (pricings != null && !pricings.isEmpty()) {
                List<AmadeusFlightOffer.FareDetailBySegment> fareDetails =
                        pricings.get(0).getFareDetailsBySegment();
                if (fareDetails != null && !fareDetails.isEmpty()) {
                    cabin = fareDetails.get(0).getCabin();
                }
            }

            return new FlightSearchResult(
                    offer.getId(),
                    "AMADEUS",
                    first.getCarrierCode() + first.getNumber(),
                    first.getCarrierCode(),
                    first.getCarrierCode(),
                    first.getDeparture().getIataCode(),
                    last.getArrival().getIataCode(),
                    first.getDeparture().getAt(),
                    last.getArrival().getAt(),
                    offer.getItineraries().get(0).getDuration(),
                    offer.getNumberOfBookableSeats(),
                    Double.parseDouble(price.getGrandTotal()),
                    price.getCurrency(),
                    cabin,
                    offer.getId(),
                    offer
            );
        }
    }
}
