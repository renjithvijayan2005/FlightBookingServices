package com.example.booking.repository;

import com.example.booking.model.BookingLuggage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingLuggageRepository extends JpaRepository<BookingLuggage, Long> {

    List<BookingLuggage> findByBooking_Id(Long bookingId);

    void deleteByBooking_Id(Long bookingId);
}
