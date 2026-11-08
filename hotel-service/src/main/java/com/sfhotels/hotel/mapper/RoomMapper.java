package com.sfhotels.hotel.mapper;

import com.sfhotels.hotel.dto.ReservationDto;
import com.sfhotels.hotel.dto.RoomDto;
import com.sfhotels.hotel.dto.RoomRequest;
import com.sfhotels.hotel.entity.Room;
import com.sfhotels.hotel.entity.RoomReservation;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper
public interface RoomMapper {

    @Mapping(target = "hotelId", source = "hotel.id")
    RoomDto toDto(Room room);

    @Mapping(target = "available", source = "available", defaultValue = "true")
    Room toEntity(RoomRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void update(RoomRequest request, @MappingTarget Room room);

    @Mapping(target = "roomId", source = "room.id")
    ReservationDto toDto(RoomReservation reservation);
}
