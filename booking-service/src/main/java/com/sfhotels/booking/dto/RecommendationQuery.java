package com.sfhotels.booking.dto;

import com.sfhotels.common.validation.DateRangeRequest;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record RecommendationQuery(
        Long hotelId,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate) implements DateRangeRequest {
}
