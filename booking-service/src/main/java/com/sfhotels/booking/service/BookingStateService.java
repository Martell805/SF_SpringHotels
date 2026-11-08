package com.sfhotels.booking.service;

import com.sfhotels.booking.dto.BookingRequest;
import com.sfhotels.booking.entity.Booking;
import com.sfhotels.booking.entity.BookingStatus;
import com.sfhotels.booking.repository.BookingRepository;
import com.sfhotels.booking.repository.UserRepository;
import com.sfhotels.common.error.ConflictException;
import com.sfhotels.common.error.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingStateService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    @Transactional
    public Booking createPending(Long userId, Long roomId, BookingRequest request, String requestId) {
        Booking booking = new Booking();
        booking.setUser(userRepository.getReferenceById(userId));
        booking.setRoomId(roomId);
        booking.setStartDate(request.startDate());
        booking.setEndDate(request.endDate());
        booking.setRequestId(requestId);
        booking.setStatus(BookingStatus.PENDING);
        bookingRepository.saveAndFlush(booking);
        log.info("bookingId={} requestId={} status -> PENDING roomId={} dates {}..{}", booking.getId(), requestId,
                roomId, request.startDate(), request.endDate());
        return booking;
    }

    @Transactional
    public Booking reassignRoom(Long bookingId, Long roomId) {
        Booking booking = get(bookingId);
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new ConflictException("Booking " + bookingId + " is no longer pending");
        }
        log.info("bookingId={} auto-select switches roomId {} -> {}", bookingId, booking.getRoomId(), roomId);
        booking.setRoomId(roomId);
        return booking;
    }

    @Transactional
    public Booking confirm(Long bookingId) {
        return transition(bookingId, BookingStatus.CONFIRMED, "hotel confirmed availability");
    }

    @Transactional
    public Booking cancel(Long bookingId, String reason) {
        Booking booking = get(bookingId);
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return booking;
        }
        return transition(booking, BookingStatus.CANCELLED, reason);
    }

    private Booking transition(Long bookingId, BookingStatus target, String reason) {
        return transition(get(bookingId), target, reason);
    }

    private Booking transition(Booking booking, BookingStatus target, String reason) {
        BookingStatus source = booking.getStatus();
        booking.transitionTo(target);
        bookingRepository.flush();
        log.info("bookingId={} status {} -> {} reason='{}'", booking.getId(), source, target, reason);
        return booking;
    }

    private Booking get(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking " + bookingId + " not found"));
    }
}
