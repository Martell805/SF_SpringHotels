package com.sfhotels.booking.service;

import com.sfhotels.booking.dto.AuthRequest;
import com.sfhotels.booking.dto.TokenResponse;
import com.sfhotels.booking.entity.Role;
import com.sfhotels.booking.entity.User;
import com.sfhotels.booking.repository.UserRepository;
import com.sfhotels.booking.security.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    @Transactional
    public TokenResponse register(AuthRequest request) {
        User user = userService.createEntity(request.username(), request.password(), Role.USER);
        log.info("User registered id={} username={}", user.getId(), user.getUsername());
        return tokenService.issueUserToken(user);
    }

    @Transactional(readOnly = true)
    public TokenResponse authenticate(AuthRequest request) {
        User user = userRepository.findByUsername(request.username())
                .filter(found -> passwordEncoder.matches(request.password(), found.getPassword()))
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));
        log.info("User authenticated id={} username={}", user.getId(), user.getUsername());
        return tokenService.issueUserToken(user);
    }
}
