package com.sfhotels.hotel.dto;

public record RoomStatsDto(
        Long roomId,
        Long hotelId,
        String number,
        boolean available,
        long timesBooked,
        long bookedNights,
        double occupancyPercent) {
}
