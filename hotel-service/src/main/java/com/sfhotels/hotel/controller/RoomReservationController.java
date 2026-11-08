package com.sfhotels.hotel.controller;

import com.sfhotels.common.security.Roles;
import com.sfhotels.hotel.dto.ReleaseRequest;
import com.sfhotels.hotel.dto.ReservationDto;
import com.sfhotels.hotel.dto.ReservationRequest;
import com.sfhotels.hotel.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Rooms (internal)", description = "Called by Booking Service only, not exposed via Gateway")
@RestController
@RequestMapping("/api/rooms/{id}")
@RequiredArgsConstructor
@PreAuthorize(Roles.HAS_INTERNAL)
public class RoomReservationController {

    private final ReservationService reservationService;

    @Operation(summary = "Confirm availability and lock the slot (idempotent by requestId)")
    @PostMapping("/confirm-availability")
    public ReservationDto confirm(@PathVariable Long id, @Valid @RequestBody ReservationRequest request) {
        return reservationService.confirm(id, request);
    }

    @Operation(summary = "Compensation: release the slot lock (idempotent by requestId)")
    @PostMapping("/release")
    public ReservationDto release(@PathVariable Long id, @Valid @RequestBody ReleaseRequest request) {
        return reservationService.release(id, request);
    }
}
