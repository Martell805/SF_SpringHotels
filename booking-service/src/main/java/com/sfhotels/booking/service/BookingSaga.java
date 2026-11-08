package com.sfhotels.booking.service;

import com.sfhotels.booking.client.HotelClient;
import com.sfhotels.booking.client.HotelFailures;
import com.sfhotels.booking.client.RoomUnavailableException;
import com.sfhotels.booking.client.dto.ReleaseRequest;
import com.sfhotels.booking.client.dto.ReservationRequest;
import com.sfhotels.booking.entity.Booking;
import com.sfhotels.common.error.ConflictException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingSaga {

    private static final String BOOKING_ID = "bookingId";

    private final HotelClient hotelClient;
    private final BookingStateService stateService;

    public Booking run(Booking booking, List<Long> candidateRoomIds) {
        try (MDC.MDCCloseable ignored = MDC.putCloseable(BOOKING_ID, String.valueOf(booking.getId()))) {
            Booking current = booking;
            for (Long roomId : candidateRoomIds) {
                current = assignRoom(current, roomId);
                if (reserve(current)) {
                    return confirmOrCompensate(current);
                }
            }
            stateService.cancel(booking.getId(), "no available room among candidates " + candidateRoomIds);
            throw new ConflictException("Room is not available for the requested dates (bookingId="
                    + booking.getId() + ")");
        }
    }

    public void compensate(Booking booking, String reason) {
        try (MDC.MDCCloseable ignored = MDC.putCloseable(BOOKING_ID, String.valueOf(booking.getId()))) {
            log.warn("bookingId={} compensation started roomId={} reason='{}'", booking.getId(), booking.getRoomId(),
                    reason);
            releaseQuietly(booking);
            stateService.cancel(booking.getId(), "compensation: " + reason);
        }
    }

    private boolean reserve(Booking booking) {
        try {
            hotelClient.confirmAvailability(booking.getRoomId(), new ReservationRequest(booking.getRequestId(),
                    booking.getId(), booking.getStartDate(), booking.getEndDate()));
            return true;
        } catch (RoomUnavailableException ex) {
            log.info("bookingId={} roomId={} is unavailable: {}", booking.getId(), booking.getRoomId(),
                    ex.getMessage());
            return false;
        } catch (RuntimeException ex) {
            compensate(booking, ex.toString());
            throw HotelFailures.translate(ex);
        }
    }

    private Booking confirmOrCompensate(Booking booking) {
        try {
            return stateService.confirm(booking.getId());
        } catch (RuntimeException ex) {
            compensate(booking, ex.toString());
            throw ex;
        }
    }

    private Booking assignRoom(Booking booking, Long roomId) {
        return booking.getRoomId().equals(roomId) ? booking : stateService.reassignRoom(booking.getId(), roomId);
    }

    private void releaseQuietly(Booking booking) {
        try {
            hotelClient.release(booking.getRoomId(), new ReleaseRequest(booking.getRequestId(), booking.getId()));
            log.info("bookingId={} compensation: slot released roomId={}", booking.getId(), booking.getRoomId());
        } catch (RuntimeException ex) {
            log.error("bookingId={} compensation: release failed roomId={} error={}", booking.getId(),
                    booking.getRoomId(), ex.toString());
        }
    }
}
