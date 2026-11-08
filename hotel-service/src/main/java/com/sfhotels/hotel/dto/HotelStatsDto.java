package com.sfhotels.hotel.dto;

import java.time.LocalDate;

public record HotelStatsDto(
        Long hotelId,
        String name,
        String city,
        LocalDate from,
        LocalDate to,
        int rooms,
        long totalTimesBooked,
        long bookedNights,
        double occupancyPercent) {
}
