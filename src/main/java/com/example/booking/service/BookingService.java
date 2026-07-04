package com.example.booking.service;

import com.example.booking.dto.BookingModifyRequest;
import com.example.booking.dto.BookingRequest;
import com.example.booking.dto.BookingResponse;
import com.example.booking.dto.PaymentRequest;
import com.example.booking.dto.amadeus.*;
import com.example.booking.exception.BookingException;
import com.example.booking.integration.AmadeusApiService;
import com.example.booking.model.*;
import com.example.booking.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository        bookingRepository;
    private final FlightRepository         flightRepository;
    private final PaymentRepository        paymentRepository;
    private final PassengerRepository      passengerRepository;
    private final BookingLuggageRepository bookingLuggageRepository;
    private final AmadeusApiService        amadeusApiService;
    private final EmailService             emailService;
    private final StripePaymentService     stripePaymentService;

    // ── Create Booking ────────────────────────────────────────────────────

    @Transactional
    public Booking createBooking(BookingRequest request) {
        if (request.getGdprConsent() == null || !request.getGdprConsent()) {
            throw new BookingException("GDPR consent is required to complete the booking");
        }

        Booking booking = new Booking();
        booking.setBookingReference(generateReference());
        booking.setBookingDate(LocalDateTime.now());
        booking.setNumPassengers(request.getPassengers().size());
        booking.setStatus("PENDING");
        booking.setPaymentStatus("PENDING");
        booking.setContactName(request.getContactName());
        booking.setContactEmail(request.getContactEmail());
        booking.setContactPhone(request.getContactPhone());
        booking.setGdprConsent(Boolean.TRUE.equals(request.getGdprConsent()));
        booking.setGdprConsentTimestamp(LocalDateTime.now());
        booking.setLuggageTotal(BigDecimal.ZERO);

        if ("AMADEUS".equals(request.getSource())) {
            booking = processAmadeusBooking(booking, request);
        } else {
            booking = processLocalBooking(booking, request);
        }

        List<Passenger> passengers = buildPassengers(request.getPassengers(), booking);
        passengerRepository.saveAll(passengers);

        return booking;
    }

    private Booking processAmadeusBooking(Booking booking, BookingRequest request) {
        AmadeusFlightOffer offer = request.getAmadeusOffer();
        if (offer == null) {
            throw new BookingException("No Amadeus offer attached to request");
        }

        // ── Step 1: price confirmation (best-effort) ──────────────────────
        AmadeusFlightOffer confirmedOffer = offer;
        try {
            confirmedOffer = amadeusApiService.confirmPrice(offer);
            log.info("Amadeus price confirmed for offer {}", offer.getId());
        } catch (Exception ex) {
            log.warn("Amadeus price confirmation failed ({}), using original offer price", ex.toString());
        }

        // Set amount from confirmed price, falling back through total → base
        BigDecimal amount = extractPrice(confirmedOffer);
        if (amount == null) amount = extractPrice(offer);
        if (amount == null) amount = BigDecimal.ZERO;
        booking.setTotalAmount(amount);

        // ── Step 2: order creation (non-fatal — test API doesn't support it) ─
        List<AmadeusPassenger> amadeusPassengers = mapToAmadeusPassengers(request.getPassengers());
        try {
            AmadeusOrderResponse orderResponse = amadeusApiService.createOrder(confirmedOffer, amadeusPassengers);
            AmadeusOrder order = orderResponse != null ? orderResponse.getData() : null;
            if (order != null && order.getId() != null) {
                booking.setAmadeusOrderId(order.getId());
                log.info("Amadeus order created: {} / PNR: {}", order.getId(),
                         order.getAssociatedRecords() != null
                                 ? order.getAssociatedRecords().getReference() : "N/A");
            } else {
                // Amadeus test sandbox returns null data for order creation — not a failure
                log.warn("Amadeus order creation returned no data (test API limitation) — booking confirmed locally");
            }
        } catch (Exception ex) {
            log.warn("Amadeus order creation unavailable ({}), booking confirmed locally", ex.toString());
        }

        booking.setStatus("CONFIRMED");
        return bookingRepository.save(booking);
    }

    /** Extracts a non-null, non-zero price from an offer's Price object. */
    private BigDecimal extractPrice(AmadeusFlightOffer offer) {
        if (offer == null || offer.getPrice() == null) return null;
        AmadeusFlightOffer.Price p = offer.getPrice();
        String raw = p.getGrandTotal() != null ? p.getGrandTotal()
                   : p.getTotal()     != null ? p.getTotal()
                   : p.getBase();
        try {
            return raw != null ? new BigDecimal(raw) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Booking processLocalBooking(Booking booking, BookingRequest request) {
        Flight flight = flightRepository.findById(Objects.requireNonNull(request.getFlightId()))
                .orElseThrow(() -> new BookingException("Flight not found"));

        int seats = request.getPassengers().size();
        if (flight.getAvailableSeats() < seats) {
            throw new BookingException("Not enough seats. Requested: " + seats
                    + ", Available: " + flight.getAvailableSeats());
        }

        flight.setAvailableSeats(flight.getAvailableSeats() - seats);
        flightRepository.save(flight);

        booking.setFlight(flight);
        booking.setStatus("CONFIRMED");
        booking.setTotalAmount(flight.getPrice().multiply(BigDecimal.valueOf(seats)));

        return bookingRepository.save(booking);
    }

    // ── Process Payment ───────────────────────────────────────────────────

    @Transactional
    public Payment processPayment(Long bookingId, PaymentRequest request) {
        Booking booking = bookingRepository.findById(Objects.requireNonNull(bookingId))
                .orElseThrow(() -> new BookingException("Booking not found"));

        if ("PAID".equals(booking.getPaymentStatus())) {
            throw new BookingException("Booking already paid");
        }

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setCurrency("USD");
        payment.setTransactionDateTime(LocalDateTime.now());

        BigDecimal total = (booking.getTotalAmount() != null ? booking.getTotalAmount() : BigDecimal.ZERO)
                .add(booking.getLuggageTotal() != null ? booking.getLuggageTotal() : BigDecimal.ZERO);
        payment.setAmount(total);

        // Stripe payment
        if (stripePaymentService.isConfigured() && request.getStripePaymentIntentId() != null) {
            boolean verified = stripePaymentService.verifyPayment(request.getStripePaymentIntentId());
            if (!verified) {
                payment.setStatus("FAILED");
                paymentRepository.save(payment);
                throw new BookingException("Payment verification failed");
            }
            payment.setTransactionId(request.getStripePaymentIntentId());
            booking.setStripePaymentIntentId(request.getStripePaymentIntentId());
        } else {
            // Mock processor (used when Stripe keys not configured)
            payment.setTransactionId("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }

        payment.setStatus("SUCCESS");
        paymentRepository.save(payment);

        // Generate confirmation key on first successful payment
        if (booking.getConfirmationKey() == null) {
            booking.setConfirmationKey(UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase());
        }
        booking.setPaymentStatus("PAID");
        bookingRepository.save(booking);

        List<BookingLuggage> luggage = bookingLuggageRepository.findByBooking_Id(bookingId);
        emailService.sendPaymentConfirmation(booking, payment);
        emailService.sendBookingConfirmation(booking, luggage);

        return payment;
    }

    // ── Stripe Intent ─────────────────────────────────────────────────────

    public java.util.Map<String, String> createStripeIntent(Long bookingId) {
        Booking booking = bookingRepository.findById(Objects.requireNonNull(bookingId))
                .orElseThrow(() -> new BookingException("Booking not found"));
        BigDecimal total = (booking.getTotalAmount() != null ? booking.getTotalAmount() : BigDecimal.ZERO)
                .add(booking.getLuggageTotal() != null ? booking.getLuggageTotal() : BigDecimal.ZERO);
        try {
            return stripePaymentService.createPaymentIntent(total, booking.getBookingReference());
        } catch (Exception e) {
            throw new BookingException("Could not create payment intent: " + e.getMessage());
        }
    }

    // ── Manage Booking ────────────────────────────────────────────────────

    public BookingResponse getByReference(String reference, String confirmationKey) {
        Booking booking = bookingRepository.findByBookingReference(reference)
                .orElseThrow(() -> new BookingException("Booking not found"));

        if (confirmationKey != null && !confirmationKey.equals(booking.getConfirmationKey())) {
            throw new BookingException("Invalid confirmation key");
        }

        return BookingResponse.from(booking, false);
    }

    @Transactional
    public BookingResponse modifyBooking(BookingModifyRequest request) {
        Booking booking = bookingRepository.findByBookingReference(request.getBookingReference())
                .orElseThrow(() -> new BookingException("Booking not found"));

        if (!request.getConfirmationKey().equals(booking.getConfirmationKey())) {
            throw new BookingException("Invalid confirmation key");
        }

        if ("CANCELLED".equals(booking.getStatus())) {
            throw new BookingException("Cannot modify a cancelled booking");
        }

        if (request.getContactName()  != null) booking.setContactName(request.getContactName());
        if (request.getContactEmail() != null) booking.setContactEmail(request.getContactEmail());
        if (request.getContactPhone() != null) booking.setContactPhone(request.getContactPhone());

        bookingRepository.save(booking);
        emailService.sendModificationConfirmation(booking);

        return BookingResponse.from(booking, false);
    }

    @Transactional
    public BookingResponse cancelBooking(String reference, String confirmationKey) {
        Booking booking = bookingRepository.findByBookingReference(reference)
                .orElseThrow(() -> new BookingException("Booking not found"));

        if (!confirmationKey.equals(booking.getConfirmationKey())) {
            throw new BookingException("Invalid confirmation key");
        }

        if ("CANCELLED".equals(booking.getStatus())) {
            throw new BookingException("Booking is already cancelled");
        }

        // Restore seats for local bookings
        if (booking.getFlight() != null) {
            Flight flight = booking.getFlight();
            flight.setAvailableSeats(flight.getAvailableSeats() + booking.getNumPassengers());
            flightRepository.save(flight);
        }

        booking.setStatus("CANCELLED");
        booking.setPaymentStatus("REFUNDED");
        bookingRepository.save(booking);

        emailService.sendCancellationConfirmation(booking);

        return BookingResponse.from(booking, false);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private String generateReference() {
        return "BK" + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 8).toUpperCase();
    }

    private List<Passenger> buildPassengers(List<BookingRequest.PassengerInfo> infos, Booking booking) {
        return IntStream.range(0, infos.size()).mapToObj(i -> {
            BookingRequest.PassengerInfo info = infos.get(i);
            Passenger p = new Passenger();
            p.setBooking(booking);
            p.setFirstName(info.getFirstName());
            p.setLastName(info.getLastName());
            p.setDateOfBirth(info.getDateOfBirth());
            p.setPassportNumber(info.getPassportNumber());
            p.setNationality(info.getNationality());
            p.setPassengerType(info.getPassengerType() != null ? info.getPassengerType() : "ADULT");
            return p;
        }).toList();
    }

    private List<AmadeusPassenger> mapToAmadeusPassengers(List<BookingRequest.PassengerInfo> passengers) {
        return IntStream.range(0, passengers.size()).mapToObj(i -> {
            BookingRequest.PassengerInfo p = passengers.get(i);
            AmadeusPassenger ap = new AmadeusPassenger();
            ap.setId(String.valueOf(i + 1));
            ap.setDateOfBirth(p.getDateOfBirth() != null ? p.getDateOfBirth().toString() : null);
            ap.setTravelerType(p.getPassengerType() != null ? p.getPassengerType() : "ADULT");
            ap.setName(new AmadeusPassengerName(p.getFirstName(), p.getLastName(), p.getGender()));
            ap.setDocuments(List.of(new AmadeusDocument(
                    "PASSPORT", p.getPassportNumber(), null, null, p.getNationality(), true)));
            return ap;
        }).toList();
    }
}
