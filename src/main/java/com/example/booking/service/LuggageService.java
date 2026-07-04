package com.example.booking.service;

import com.example.booking.dto.LuggageSelectionRequest;
import com.example.booking.exception.BookingException;
import com.example.booking.model.*;
import com.example.booking.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LuggageService {

    private final LuggageOptionRepository  luggageOptionRepository;
    private final BookingLuggageRepository bookingLuggageRepository;
    private final BookingRepository        bookingRepository;
    private final PassengerRepository      passengerRepository;

    public List<LuggageOption> getAllOptions() {
        return luggageOptionRepository.findByActiveTrue();
    }

    @Transactional
    public List<BookingLuggage> saveSelection(LuggageSelectionRequest request) {
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new BookingException("Booking not found"));

        // Clear previous selection
        bookingLuggageRepository.deleteByBooking_Id(booking.getId());

        BigDecimal total = BigDecimal.ZERO;
        List<BookingLuggage> saved = new java.util.ArrayList<>();

        for (LuggageSelectionRequest.LuggageItem item : request.getItems()) {
            LuggageOption option = luggageOptionRepository.findById(item.getLuggageOptionId())
                    .orElseThrow(() -> new BookingException("Luggage option not found: " + item.getLuggageOptionId()));

            Passenger passenger = null;
            if (item.getPassengerId() != null) {
                passenger = passengerRepository.findById(item.getPassengerId()).orElse(null);
            }

            BookingLuggage bl = new BookingLuggage();
            bl.setBooking(booking);
            bl.setPassenger(passenger);
            bl.setLuggageOption(option);
            bl.setQuantity(item.getQuantity());
            bl.setPricePerUnit(option.getPrice());
            saved.add(bookingLuggageRepository.save(bl));

            total = total.add(option.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        booking.setLuggageTotal(total);
        bookingRepository.save(booking);

        return saved;
    }

    public List<BookingLuggage> getByBookingId(Long bookingId) {
        return bookingLuggageRepository.findByBooking_Id(bookingId);
    }
}
