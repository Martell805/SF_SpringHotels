package com.sfhotels.hotel.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record RoomRequest(
        @NotNull Long hotelId,
        @NotBlank @Size(max = 20) String number,
        @NotNull @Min(1) @Max(10) Integer capacity,
        @NotNull @DecimalMin("0.0") BigDecimal pricePerNight,
        Boolean available) {
}
