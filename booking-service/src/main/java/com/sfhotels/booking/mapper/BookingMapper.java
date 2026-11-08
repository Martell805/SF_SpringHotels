package com.sfhotels.booking.mapper;

import com.sfhotels.booking.dto.BookingDto;
import com.sfhotels.booking.entity.Booking;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface BookingMapper {

    @Mapping(target = "userId", source = "user.id")
    BookingDto toDto(Booking booking);
}
