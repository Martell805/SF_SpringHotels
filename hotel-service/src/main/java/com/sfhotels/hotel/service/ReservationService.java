package com.sfhotels.hotel.service;

import com.sfhotels.common.error.ConflictException;
import com.sfhotels.common.error.NotFoundException;
import com.sfhotels.hotel.dto.ReleaseRequest;
import com.sfhotels.hotel.dto.ReservationDto;
import com.sfhotels.hotel.dto.ReservationRequest;
import com.sfhotels.hotel.entity.ReservationStatus;
import com.sfhotels.hotel.entity.Room;
import com.sfhotels.hotel.entity.RoomReservation;
import com.sfhotels.hotel.mapper.RoomMapper;
import com.sfhotels.hotel.repository.RoomRepository;
import com.sfhotels.hotel.repository.RoomReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {

    private final RoomRepository roomRepository;
    private final RoomReservationRepository reservationRepository;
    private final RoomMapper roomMapper;

    @Transactional
    public ReservationDto confirm(Long roomId, ReservationRequest request) {
        Room room = lockRoom(roomId);
        return reservationRepository.findByRequestId(request.requestId())
                .map(existing -> replayConfirm(existing, roomId))
                .orElseGet(() -> reserve(room, request));
    }

    @Transactional
    public ReservationDto release(Long roomId, ReleaseRequest request) {
        Room room = lockRoom(roomId);
        RoomReservation reservation = reservationRepository.findByRequestId(request.requestId())
                .orElseGet(() -> storeReleaseMarker(room, request));
        ensureSameRoom(reservation, roomId);
        if (reservation.isActive()) {
            reservation.setStatus(ReservationStatus.RELEASED);
            room.decrementTimesBooked();
            log.info("bookingId={} requestId={} roomId={} slot released", reservation.getBookingId(),
                    request.requestId(), roomId);
        } else {
            log.info("bookingId={} requestId={} roomId={} release skipped: already released",
                    reservation.getBookingId(), request.requestId(), roomId);
        }
        return roomMapper.toDto(reservation);
    }

    private ReservationDto reserve(Room room, ReservationRequest request) {
        if (!room.isAvailable()) {
            throw new ConflictException("Room " + room.getId() + " is out of service");
        }
        boolean occupied = reservationRepository.existsOverlapping(room.getId(), ReservationStatus.ACTIVE,
                request.startDate(), request.endDate());
        if (occupied) {
            log.info("bookingId={} requestId={} roomId={} rejected: dates {}..{} are occupied",
                    request.bookingId(), request.requestId(), room.getId(), request.startDate(), request.endDate());
            throw new ConflictException("Room " + room.getId() + " is already booked for the requested dates");
        }
        RoomReservation reservation = new RoomReservation();
        reservation.setRoom(room);
        reservation.setRequestId(request.requestId());
        reservation.setBookingId(request.bookingId());
        reservation.setStartDate(request.startDate());
        reservation.setEndDate(request.endDate());
        reservation.setStatus(ReservationStatus.ACTIVE);
        reservationRepository.save(reservation);
        room.incrementTimesBooked();
        log.info("bookingId={} requestId={} roomId={} slot locked {}..{} timesBooked={}", request.bookingId(),
                request.requestId(), room.getId(), request.startDate(), request.endDate(), room.getTimesBooked());
        return roomMapper.toDto(reservation);
    }

    private ReservationDto replayConfirm(RoomReservation existing, Long roomId) {
        ensureSameRoom(existing, roomId);
        if (!existing.isActive()) {
            throw new ConflictException("Request " + existing.getRequestId() + " was already released");
        }
        log.info("bookingId={} requestId={} roomId={} duplicate confirm, returning existing reservation",
                existing.getBookingId(), existing.getRequestId(), roomId);
        return roomMapper.toDto(existing);
    }

    private RoomReservation storeReleaseMarker(Room room, ReleaseRequest request) {
        RoomReservation marker = new RoomReservation();
        marker.setRoom(room);
        marker.setRequestId(request.requestId());
        marker.setBookingId(request.bookingId());
        marker.setStatus(ReservationStatus.RELEASED);
        log.info("bookingId={} requestId={} roomId={} release before confirm: marker stored",
                request.bookingId(), request.requestId(), room.getId());
        return reservationRepository.save(marker);
    }

    private void ensureSameRoom(RoomReservation reservation, Long roomId) {
        if (!reservation.getRoom().getId().equals(roomId)) {
            throw new ConflictException("Request " + reservation.getRequestId() + " belongs to another room");
        }
    }

    private Room lockRoom(Long roomId) {
        return roomRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> new NotFoundException("Room " + roomId + " not found"));
    }
}
