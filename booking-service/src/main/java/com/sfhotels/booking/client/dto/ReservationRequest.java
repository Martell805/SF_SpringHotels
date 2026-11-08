package com.sfhotels.booking.client.dto;

import java.time.LocalDate;

public record ReservationRequest(String requestId, Long bookingId, LocalDate startDate, LocalDate endDate) {
}
