package com.sfhotels.booking.service;

import com.sfhotels.booking.config.BookingProperties;
import com.sfhotels.booking.entity.Booking;
import com.sfhotels.booking.entity.BookingStatus;
import com.sfhotels.booking.repository.BookingRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PendingBookingReaper {

    private final BookingRepository bookingRepository;
    private final BookingSaga saga;
    private final BookingProperties properties;

    @Scheduled(fixedDelayString = "${app.booking.reaper-interval:PT30S}")
    public void cancelStalePending() {
        Instant threshold = Instant.now().minus(properties.pendingTimeout());
        bookingRepository.findByStatusAndCreatedAtBefore(BookingStatus.PENDING, threshold)
                .forEach(this::compensateSafely);
    }

    private void compensateSafely(Booking booking) {
        try {
            saga.compensate(booking, "pending timeout exceeded");
        } catch (RuntimeException ex) {
            log.warn("bookingId={} stale pending compensation skipped: {}", booking.getId(), ex.getMessage());
        }
    }
}
