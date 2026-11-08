package com.sfhotels.booking.dto;

import com.sfhotels.booking.entity.Role;
import java.time.Instant;

public record UserDto(Long id, String username, Role role, Instant createdAt) {
}
