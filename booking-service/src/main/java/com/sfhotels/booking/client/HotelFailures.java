package com.sfhotels.booking.client;

import com.sfhotels.common.error.ApiException;
import com.sfhotels.common.error.ServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;

public final class HotelFailures {

    private HotelFailures() {
    }

    public static ApiException translate(RuntimeException failure) {
        if (failure instanceof ApiException apiException) {
            return apiException;
        }
        if (failure instanceof CallNotPermittedException) {
            return new ServiceUnavailableException("Hotel service is temporarily unavailable (circuit open)");
        }
        return new ServiceUnavailableException("Hotel service is unavailable: " + failure.getClass().getSimpleName());
    }
}
