package com.sfhotels.hotel.mapper;

import com.sfhotels.hotel.dto.HotelDto;
import com.sfhotels.hotel.dto.HotelRequest;
import com.sfhotels.hotel.entity.Hotel;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper
public interface HotelMapper {

    HotelDto toDto(Hotel hotel);

    Hotel toEntity(HotelRequest request);

    void update(HotelRequest request, @MappingTarget Hotel hotel);
}
