package com.sfhotels.hotel.dto;

import com.sfhotels.hotel.entity.ReservationStatus;
import java.time.LocalDate;

public record ReservationDto(
        Long id,
        Long roomId,
        String requestId,
        Long bookingId,
        LocalDate startDate,
        LocalDate endDate,
        ReservationStatus status) {
}
