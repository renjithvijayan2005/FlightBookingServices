package com.example.booking.repository;

import com.example.booking.model.LuggageOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LuggageOptionRepository extends JpaRepository<LuggageOption, Long> {

    List<LuggageOption> findByActiveTrue();

    List<LuggageOption> findByCategoryAndActiveTrue(String category);
}
