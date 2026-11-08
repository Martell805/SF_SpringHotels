package com.sfhotels.booking.mapper;

import com.sfhotels.booking.dto.UserDto;
import com.sfhotels.booking.entity.User;
import org.mapstruct.Mapper;

@Mapper
public interface UserMapper {

    UserDto toDto(User user);
}
