package com.sfhotels.hotel.service;

import com.sfhotels.common.error.NotFoundException;
import com.sfhotels.common.web.PageResponse;
import com.sfhotels.hotel.dto.HotelDto;
import com.sfhotels.hotel.dto.HotelRequest;
import com.sfhotels.hotel.entity.Hotel;
import com.sfhotels.hotel.mapper.HotelMapper;
import com.sfhotels.hotel.repository.HotelRepository;
import com.sfhotels.hotel.repository.HotelSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class HotelService {

    private final HotelRepository hotelRepository;
    private final HotelMapper hotelMapper;

    @Transactional(readOnly = true)
    public PageResponse<HotelDto> search(String name, String city, Pageable pageable) {
        Specification<Hotel> filter = Specification.allOf(
                HotelSpecifications.nameContains(name),
                HotelSpecifications.cityContains(city));
        return PageResponse.of(hotelRepository.findAll(filter, pageable).map(hotelMapper::toDto));
    }

    @Transactional(readOnly = true)
    public HotelDto get(Long id) {
        return hotelMapper.toDto(getEntity(id));
    }

    @Transactional
    public HotelDto create(HotelRequest request) {
        Hotel hotel = hotelRepository.save(hotelMapper.toEntity(request));
        log.info("Hotel created id={} name={}", hotel.getId(), hotel.getName());
        return hotelMapper.toDto(hotel);
    }

    @Transactional
    public HotelDto update(Long id, HotelRequest request) {
        Hotel hotel = getEntity(id);
        hotelMapper.update(request, hotel);
        log.info("Hotel updated id={}", id);
        return hotelMapper.toDto(hotel);
    }

    @Transactional
    public void delete(Long id) {
        hotelRepository.delete(getEntity(id));
        hotelRepository.flush();
        log.info("Hotel deleted id={}", id);
    }

    Hotel getEntity(Long id) {
        return hotelRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Hotel " + id + " not found"));
    }
}
