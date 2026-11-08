package com.sfhotels.hotel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record HotelRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 255) String address,
        @NotBlank @Size(max = 80) String city) {
}
