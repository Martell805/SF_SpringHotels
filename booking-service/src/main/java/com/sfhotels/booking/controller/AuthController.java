package com.sfhotels.booking.controller;

import com.sfhotels.booking.dto.AuthRequest;
import com.sfhotels.booking.dto.TokenResponse;
import com.sfhotels.booking.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentication")
@SecurityRequirements
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Register a new USER and get JWT (valid 1 hour)")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public TokenResponse register(@Valid @RequestBody AuthRequest request) {
        return authService.register(request);
    }

    @Operation(summary = "Authenticate and get JWT (valid 1 hour)")
    @PostMapping("/auth")
    public TokenResponse authenticate(@Valid @RequestBody AuthRequest request) {
        return authService.authenticate(request);
    }
}
