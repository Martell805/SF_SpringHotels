package com.sfhotels.hotel.dto;

import com.sfhotels.common.validation.DateRangeRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record ReservationRequest(
        @NotBlank @Size(max = 64) String requestId,
        @NotNull Long bookingId,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate) implements DateRangeRequest {
}
