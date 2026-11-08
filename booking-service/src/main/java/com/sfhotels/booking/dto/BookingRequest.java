package com.sfhotels.booking.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sfhotels.common.validation.DateRangeRequest;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public record BookingRequest(
        Long roomId,
        Long hotelId,
        boolean autoSelect,
        @NotNull @FutureOrPresent LocalDate startDate,
        @NotNull LocalDate endDate,
        @Size(max = 64) @Pattern(regexp = "[A-Za-z0-9._:-]+") String requestId) implements DateRangeRequest {

    public static final int MAX_NIGHTS = 30;

    @JsonIgnore
    @AssertTrue(message = "roomId is required when autoSelect is false")
    public boolean isRoomSpecified() {
        return autoSelect || roomId != null;
    }

    @JsonIgnore
    @AssertTrue(message = "stay must not exceed " + MAX_NIGHTS + " nights")
    public boolean isStayLengthValid() {
        return startDate == null || endDate == null || ChronoUnit.DAYS.between(startDate, endDate) <= MAX_NIGHTS;
    }
}
