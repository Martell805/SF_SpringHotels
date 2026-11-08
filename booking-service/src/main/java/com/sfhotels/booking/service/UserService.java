package com.sfhotels.booking.service;

import com.sfhotels.booking.dto.UserCreateRequest;
import com.sfhotels.booking.dto.UserDto;
import com.sfhotels.booking.dto.UserUpdateRequest;
import com.sfhotels.booking.entity.Role;
import com.sfhotels.booking.entity.User;
import com.sfhotels.booking.mapper.UserMapper;
import com.sfhotels.booking.repository.BookingRepository;
import com.sfhotels.booking.repository.UserRepository;
import com.sfhotels.common.error.ConflictException;
import com.sfhotels.common.error.NotFoundException;
import com.sfhotels.common.web.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public PageResponse<UserDto> list(Pageable pageable) {
        return PageResponse.of(userRepository.findAll(pageable).map(userMapper::toDto));
    }

    @Transactional(readOnly = true)
    public UserDto get(Long id) {
        return userMapper.toDto(getEntity(id));
    }

    @Transactional
    public UserDto create(UserCreateRequest request) {
        User user = createEntity(request.username(), request.password(), request.role());
        log.info("User created by admin id={} username={} role={}", user.getId(), user.getUsername(), user.getRole());
        return userMapper.toDto(user);
    }

    @Transactional
    public UserDto update(Long id, UserUpdateRequest request) {
        User user = getEntity(id);
        if (request.username() != null && !request.username().equals(user.getUsername())) {
            ensureUsernameFree(request.username());
            user.setUsername(request.username());
        }
        if (request.password() != null) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }
        if (request.role() != null) {
            user.setRole(request.role());
        }
        log.info("User updated id={}", id);
        return userMapper.toDto(user);
    }

    @Transactional
    public void delete(Long id) {
        User user = getEntity(id);
        if (bookingRepository.existsByUserId(id)) {
            throw new ConflictException("User " + id + " has bookings and cannot be deleted");
        }
        userRepository.delete(user);
        log.info("User deleted id={}", id);
    }

    User createEntity(String username, String rawPassword, Role role) {
        ensureUsernameFree(username);
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        return userRepository.saveAndFlush(user);
    }

    private void ensureUsernameFree(String username) {
        if (userRepository.existsByUsername(username)) {
            throw new ConflictException("Username '" + username + "' is already taken");
        }
    }

    private User getEntity(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new NotFoundException("User " + id + " not found"));
    }
}
