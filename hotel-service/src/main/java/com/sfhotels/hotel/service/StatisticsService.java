package com.sfhotels.hotel.service;

import com.sfhotels.hotel.dto.HotelStatsDto;
import com.sfhotels.hotel.dto.RoomStatsDto;
import com.sfhotels.hotel.entity.Hotel;
import com.sfhotels.hotel.entity.ReservationStatus;
import com.sfhotels.hotel.entity.Room;
import com.sfhotels.hotel.entity.RoomReservation;
import com.sfhotels.hotel.repository.HotelRepository;
import com.sfhotels.hotel.repository.RoomRepository;
import com.sfhotels.hotel.repository.RoomReservationRepository;
import com.sfhotels.hotel.repository.RoomSpecifications;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final RoomReservationRepository reservationRepository;

    @Transactional(readOnly = true)
    public List<RoomStatsDto> roomStats(StatsPeriod period, Long hotelId) {
        Map<Long, Long> nights = bookedNightsByRoom(period);
        return roomRepository.findAll(RoomSpecifications.inHotel(hotelId), Sort.by("id")).stream()
                .map(room -> toRoomStats(room, nights.getOrDefault(room.getId(), 0L), period))
                .sorted(Comparator.comparingDouble(RoomStatsDto::occupancyPercent).reversed())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HotelStatsDto> hotelStats(StatsPeriod period) {
        Map<Long, Long> nights = bookedNightsByRoom(period);
        return hotelRepository.findAll(Sort.by("id")).stream()
                .map(hotel -> toHotelStats(hotel, nights, period))
                .sorted(Comparator.comparingDouble(HotelStatsDto::occupancyPercent).reversed())
                .toList();
    }

    private Map<Long, Long> bookedNightsByRoom(StatsPeriod period) {
        return reservationRepository.findOverlappingPeriod(ReservationStatus.ACTIVE, period.from(), period.to())
                .stream()
                .collect(Collectors.groupingBy(reservation -> reservation.getRoom().getId(),
                        Collectors.summingLong(reservation -> overlap(reservation, period))));
    }

    private long overlap(RoomReservation reservation, StatsPeriod period) {
        return period.overlapNights(reservation.getStartDate(), reservation.getEndDate());
    }

    private RoomStatsDto toRoomStats(Room room, long bookedNights, StatsPeriod period) {
        return new RoomStatsDto(room.getId(), room.getHotel().getId(), room.getNumber(), room.isAvailable(),
                room.getTimesBooked(), bookedNights, period.occupancyPercent(bookedNights, 1));
    }

    private HotelStatsDto toHotelStats(Hotel hotel, Map<Long, Long> nights, StatsPeriod period) {
        List<Room> rooms = hotel.getRooms();
        long timesBooked = rooms.stream().mapToLong(Room::getTimesBooked).sum();
        long bookedNights = rooms.stream().mapToLong(room -> nights.getOrDefault(room.getId(), 0L)).sum();
        return new HotelStatsDto(hotel.getId(), hotel.getName(), hotel.getCity(), period.from(), period.to(),
                rooms.size(), timesBooked, bookedNights, period.occupancyPercent(bookedNights, rooms.size()));
    }
}
