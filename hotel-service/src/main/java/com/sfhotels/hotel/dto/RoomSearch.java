package com.sfhotels.hotel.dto;

import com.sfhotels.common.validation.DateRangeRequest;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;

public record RoomSearch(
        Long hotelId,
        @Min(1) Integer minCapacity,
        LocalDate startDate,
        LocalDate endDate) implements DateRangeRequest {

    @AssertTrue(message = "startDate and endDate must be provided together")
    public boolean isDatesComplete() {
        return (startDate == null) == (endDate == null);
    }
}
