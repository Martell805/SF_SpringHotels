package com.sfhotels.booking.dto;

import com.sfhotels.booking.entity.BookingStatus;
import java.time.Instant;
import java.time.LocalDate;

public record BookingDto(
        Long id,
        Long userId,
        Long roomId,
        LocalDate startDate,
        LocalDate endDate,
        BookingStatus status,
        String requestId,
        Instant createdAt,
        Instant updatedAt) {
}
