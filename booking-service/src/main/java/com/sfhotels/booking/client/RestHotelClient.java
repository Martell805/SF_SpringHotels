package com.sfhotels.booking.client;

import com.sfhotels.booking.client.dto.ReleaseRequest;
import com.sfhotels.booking.client.dto.ReservationRequest;
import com.sfhotels.booking.client.dto.RoomDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class RestHotelClient implements HotelClient {

    static final String RESILIENCE_NAME = "hotel";

    private final RestClient hotelRestClient;

    @Override
    @Retry(name = RESILIENCE_NAME)
    @CircuitBreaker(name = RESILIENCE_NAME)
    public List<RoomDto> recommend(Long hotelId, LocalDate startDate, LocalDate endDate) {
        return hotelRestClient.get()
                .uri(uri -> uri.path("/api/rooms/recommend")
                        .queryParamIfPresent("hotelId", Optional.ofNullable(hotelId))
                        .queryParam("startDate", startDate)
                        .queryParam("endDate", endDate)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<List<RoomDto>>() {
                });
    }

    @Override
    @Retry(name = RESILIENCE_NAME)
    @CircuitBreaker(name = RESILIENCE_NAME)
    public void confirmAvailability(Long roomId, ReservationRequest request) {
        log.info("bookingId={} requestId={} calling confirm-availability roomId={}",
                request.bookingId(), request.requestId(), roomId);
        hotelRestClient.post()
                .uri("/api/rooms/{id}/confirm-availability", roomId)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    @Retry(name = RESILIENCE_NAME)
    public void release(Long roomId, ReleaseRequest request) {
        log.info("bookingId={} requestId={} calling release roomId={}",
                request.bookingId(), request.requestId(), roomId);
        hotelRestClient.post()
                .uri("/api/rooms/{id}/release", roomId)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }
}
