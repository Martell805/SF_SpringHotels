package com.sfhotels.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        String traceId,
        Map<String, String> details) {

    public static ApiError of(HttpStatus status, String message, String path, Map<String, String> details) {
        return new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, path,
                MDC.get("traceId"), details);
    }

    public static ApiError of(HttpStatus status, String message, String path) {
        return of(status, message, path, Map.of());
    }
}
