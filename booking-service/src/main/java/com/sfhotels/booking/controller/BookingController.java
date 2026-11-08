package com.sfhotels.booking.controller;

import com.sfhotels.booking.client.dto.RoomDto;
import com.sfhotels.booking.dto.BookingDto;
import com.sfhotels.booking.dto.BookingRequest;
import com.sfhotels.booking.dto.RecommendationQuery;
import com.sfhotels.booking.security.CurrentUser;
import com.sfhotels.booking.service.BookingService;
import com.sfhotels.common.security.Roles;
import com.sfhotels.common.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Bookings")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@PreAuthorize(Roles.HAS_USER_OR_ADMIN)
public class BookingController {

    private final BookingService bookingService;

    @Operation(summary = "Create booking for a room or with autoSelect (idempotent by requestId)")
    @PostMapping("/booking")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingDto create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody BookingRequest request) {
        return bookingService.create(CurrentUser.from(jwt), request);
    }

    @Operation(summary = "Get own booking by id")
    @GetMapping("/booking/{id}")
    public BookingDto get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return bookingService.get(id, CurrentUser.from(jwt));
    }

    @Operation(summary = "Cancel own booking and release the room slot")
    @DeleteMapping("/booking/{id}")
    public BookingDto cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return bookingService.cancel(id, CurrentUser.from(jwt));
    }

    @Operation(summary = "Own booking history, newest first")
    @GetMapping("/bookings")
    public PageResponse<BookingDto> history(@AuthenticationPrincipal Jwt jwt,
                                            @ParameterObject @PageableDefault(size = 10, sort = "createdAt",
                                                    direction = Sort.Direction.DESC) Pageable pageable) {
        return bookingService.history(CurrentUser.from(jwt), pageable);
    }

    @Operation(summary = "Recommended free rooms: least booked first (times_booked asc, id asc)")
    @GetMapping("/bookings/recommendations")
    public List<RoomDto> recommendations(@Valid @ParameterObject RecommendationQuery query) {
        return bookingService.recommend(query);
    }
}
