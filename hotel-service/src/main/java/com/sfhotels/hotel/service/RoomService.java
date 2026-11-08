package com.sfhotels.hotel.service;

import com.sfhotels.common.error.NotFoundException;
import com.sfhotels.hotel.dto.RoomDto;
import com.sfhotels.hotel.dto.RoomRequest;
import com.sfhotels.hotel.dto.RoomSearch;
import com.sfhotels.hotel.entity.Room;
import com.sfhotels.hotel.mapper.RoomMapper;
import com.sfhotels.hotel.repository.RoomRepository;
import com.sfhotels.hotel.repository.RoomSpecifications;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomService {

    static final Sort BY_ID = Sort.by("id");
    static final Sort LEAST_BOOKED_FIRST = Sort.by("timesBooked", "id");

    private final RoomRepository roomRepository;
    private final HotelService hotelService;
    private final RoomMapper roomMapper;

    @Transactional(readOnly = true)
    public List<RoomDto> findFree(RoomSearch search) {
        return findFree(search, BY_ID);
    }

    @Transactional(readOnly = true)
    public List<RoomDto> recommend(RoomSearch search) {
        return findFree(search, LEAST_BOOKED_FIRST);
    }

    @Transactional(readOnly = true)
    public RoomDto get(Long id) {
        return roomMapper.toDto(getEntity(id));
    }

    @Transactional
    public RoomDto create(RoomRequest request) {
        Room room = roomMapper.toEntity(request);
        room.setHotel(hotelService.getEntity(request.hotelId()));
        roomRepository.saveAndFlush(room);
        log.info("Room created id={} hotelId={} number={}", room.getId(), request.hotelId(), room.getNumber());
        return roomMapper.toDto(room);
    }

    @Transactional
    public RoomDto update(Long id, RoomRequest request) {
        Room room = getEntity(id);
        roomMapper.update(request, room);
        room.setHotel(hotelService.getEntity(request.hotelId()));
        roomRepository.flush();
        log.info("Room updated id={}", id);
        return roomMapper.toDto(room);
    }

    @Transactional
    public void delete(Long id) {
        roomRepository.delete(getEntity(id));
        roomRepository.flush();
        log.info("Room deleted id={}", id);
    }

    private List<RoomDto> findFree(RoomSearch search, Sort sort) {
        Specification<Room> filter = Specification.allOf(
                RoomSpecifications.operational(),
                RoomSpecifications.inHotel(search.hotelId()),
                RoomSpecifications.minCapacity(search.minCapacity()),
                RoomSpecifications.freeBetween(search.startDate(), search.endDate()));
        return roomRepository.findAll(filter, sort).stream().map(roomMapper::toDto).toList();
    }

    private Room getEntity(Long id) {
        return roomRepository.findById(id).orElseThrow(() -> new NotFoundException("Room " + id + " not found"));
    }
}
