package com.example.booking.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class LuggageSelectionRequest {

    @NotNull
    private Long bookingId;

    @NotNull
    private List<LuggageItem> items;

    @Data
    @NoArgsConstructor
    public static class LuggageItem {
        @NotNull
        private Long luggageOptionId;

        @Min(1)
        private int quantity = 1;

        private Long passengerId;
    }
}
