package com.sfhotels.booking.controller;

import com.sfhotels.booking.dto.UserCreateRequest;
import com.sfhotels.booking.dto.UserDto;
import com.sfhotels.booking.dto.UserUpdateRequest;
import com.sfhotels.booking.service.UserService;
import com.sfhotels.common.security.Roles;
import com.sfhotels.common.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Users (admin)")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@PreAuthorize(Roles.HAS_ADMIN)
public class UserController {

    private final UserService userService;

    @Operation(summary = "List users")
    @GetMapping
    public PageResponse<UserDto> list(@ParameterObject @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return userService.list(pageable);
    }

    @Operation(summary = "Get user by id")
    @GetMapping("/{id}")
    public UserDto get(@PathVariable Long id) {
        return userService.get(id);
    }

    @Operation(summary = "Create user with any role")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto create(@Valid @RequestBody UserCreateRequest request) {
        return userService.create(request);
    }

    @Operation(summary = "Partially update user")
    @PatchMapping("/{id}")
    public UserDto update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        return userService.update(id, request);
    }

    @Operation(summary = "Delete user without bookings")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        userService.delete(id);
    }
}
