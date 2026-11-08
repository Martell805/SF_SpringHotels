package com.sfhotels.booking.service;

import com.sfhotels.booking.client.HotelClient;
import com.sfhotels.booking.client.HotelFailures;
import com.sfhotels.booking.client.dto.ReleaseRequest;
import com.sfhotels.booking.client.dto.RoomDto;
import com.sfhotels.booking.config.BookingProperties;
import com.sfhotels.booking.dto.BookingDto;
import com.sfhotels.booking.dto.BookingRequest;
import com.sfhotels.booking.dto.RecommendationQuery;
import com.sfhotels.booking.entity.Booking;
import com.sfhotels.booking.mapper.BookingMapper;
import com.sfhotels.booking.repository.BookingRepository;
import com.sfhotels.booking.security.CurrentUser;
import com.sfhotels.common.error.ConflictException;
import com.sfhotels.common.error.NotFoundException;
import com.sfhotels.common.web.PageResponse;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BookingStateService stateService;
    private final BookingSaga saga;
    private final HotelClient hotelClient;
    private final BookingMapper bookingMapper;
    private final BookingProperties properties;

    public BookingDto create(CurrentUser user, BookingRequest request) {
        String requestId = Optional.ofNullable(request.requestId()).orElseGet(() -> UUID.randomUUID().toString());
        Optional<Booking> existing = bookingRepository.findByRequestId(requestId);
        if (existing.isPresent()) {
            return replay(existing.get(), user);
        }
        List<Long> candidates = candidateRooms(request);
        Booking pending;
        try {
            pending = stateService.createPending(user.id(), candidates.get(0), request, requestId);
        } catch (DataIntegrityViolationException ex) {
            return bookingRepository.findByRequestId(requestId).map(found -> replay(found, user)).orElseThrow(() -> ex);
        }
        return bookingMapper.toDto(saga.run(pending, candidates));
    }

    public List<RoomDto> recommend(RecommendationQuery query) {
        try {
            return hotelClient.recommend(query.hotelId(), query.startDate(), query.endDate());
        } catch (RuntimeException ex) {
            throw HotelFailures.translate(ex);
        }
    }

    public PageResponse<BookingDto> history(CurrentUser user, Pageable pageable) {
        return PageResponse.of(bookingRepository.findByUserId(user.id(), pageable).map(bookingMapper::toDto));
    }

    public BookingDto get(Long id, CurrentUser user) {
        return bookingMapper.toDto(getAccessible(id, user));
    }

    public BookingDto cancel(Long id, CurrentUser user) {
        Booking booking = getAccessible(id, user);
        return switch (booking.getStatus()) {
            case CANCELLED -> bookingMapper.toDto(booking);
            case PENDING -> throw new ConflictException("Booking " + id + " is being processed, try again later");
            case CONFIRMED -> bookingMapper.toDto(releaseAndCancel(booking, user));
        };
    }

    private Booking releaseAndCancel(Booking booking, CurrentUser user) {
        try {
            hotelClient.release(booking.getRoomId(), new ReleaseRequest(booking.getRequestId(), booking.getId()));
        } catch (RuntimeException ex) {
            throw HotelFailures.translate(ex);
        }
        return stateService.cancel(booking.getId(), "cancelled by " + user.username());
    }

    private List<Long> candidateRooms(BookingRequest request) {
        if (!request.autoSelect()) {
            return List.of(request.roomId());
        }
        List<Long> candidates = recommend(new RecommendationQuery(request.hotelId(), request.startDate(),
                request.endDate()))
                .stream()
                .limit(properties.autoSelectAttempts())
                .map(RoomDto::id)
                .toList();
        if (candidates.isEmpty()) {
            throw new ConflictException("No free rooms for the requested dates");
        }
        log.info("Auto-select candidates (least booked first): {}", candidates);
        return candidates;
    }

    private BookingDto replay(Booking booking, CurrentUser user) {
        if (!booking.isOwnedBy(user.id())) {
            throw new ConflictException("requestId is already used by another booking");
        }
        log.info("bookingId={} requestId={} duplicate request, returning existing booking status={}",
                booking.getId(), booking.getRequestId(), booking.getStatus());
        return bookingMapper.toDto(booking);
    }

    private Booking getAccessible(Long id, CurrentUser user) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Booking " + id + " not found"));
        if (!user.canAccess(booking.getUser().getId())) {
            throw new AccessDeniedException("Booking " + id + " belongs to another user");
        }
        return booking;
    }
}
