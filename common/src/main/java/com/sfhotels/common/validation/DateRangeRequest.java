package com.sfhotels.common.validation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import java.time.LocalDate;

public interface DateRangeRequest {

    LocalDate startDate();

    LocalDate endDate();

    @JsonIgnore
    @AssertTrue(message = "startDate must be before endDate")
    default boolean isDateRangeValid() {
        return startDate() == null || endDate() == null || startDate().isBefore(endDate());
    }
}
