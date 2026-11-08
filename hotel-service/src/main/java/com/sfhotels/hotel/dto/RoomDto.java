package com.sfhotels.hotel.dto;

import java.math.BigDecimal;

public record RoomDto(
        Long id,
        Long hotelId,
        String number,
        int capacity,
        BigDecimal pricePerNight,
        boolean available,
        long timesBooked) {
}
