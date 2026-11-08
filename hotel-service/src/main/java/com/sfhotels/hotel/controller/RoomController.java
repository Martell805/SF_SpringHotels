package com.sfhotels.hotel.controller;

import com.sfhotels.common.security.Roles;
import com.sfhotels.hotel.dto.RoomDto;
import com.sfhotels.hotel.dto.RoomRequest;
import com.sfhotels.hotel.dto.RoomSearch;
import com.sfhotels.hotel.dto.RoomStatsDto;
import com.sfhotels.hotel.service.RoomService;
import com.sfhotels.hotel.service.StatisticsService;
import com.sfhotels.hotel.service.StatsPeriod;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Rooms")
@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;
    private final StatisticsService statisticsService;

    @Operation(summary = "Free rooms (operational and not booked for the dates, if given)")
    @GetMapping
    @PreAuthorize(Roles.HAS_USER_OR_ADMIN)
    public List<RoomDto> findFree(@Valid @ParameterObject RoomSearch search) {
        return roomService.findFree(search);
    }

    @Operation(summary = "Free rooms sorted by times_booked ascending, then by id")
    @GetMapping("/recommend")
    @PreAuthorize(Roles.HAS_ANY_ROLE)
    public List<RoomDto> recommend(@Valid @ParameterObject RoomSearch search) {
        return roomService.recommend(search);
    }

    @Operation(summary = "Occupancy statistics per room for the period (default: next 30 days)")
    @GetMapping("/stats")
    @PreAuthorize(Roles.HAS_ADMIN)
    public List<RoomStatsDto> stats(@RequestParam(required = false) LocalDate from,
                                    @RequestParam(required = false) LocalDate to,
                                    @RequestParam(required = false) Long hotelId) {
        return statisticsService.roomStats(StatsPeriod.of(from, to), hotelId);
    }

    @Operation(summary = "Get room by id")
    @GetMapping("/{id}")
    @PreAuthorize(Roles.HAS_USER_OR_ADMIN)
    public RoomDto get(@PathVariable Long id) {
        return roomService.get(id);
    }

    @Operation(summary = "Add room to hotel")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Roles.HAS_ADMIN)
    public RoomDto create(@Valid @RequestBody RoomRequest request) {
        return roomService.create(request);
    }

    @Operation(summary = "Update room")
    @PutMapping("/{id}")
    @PreAuthorize(Roles.HAS_ADMIN)
    public RoomDto update(@PathVariable Long id, @Valid @RequestBody RoomRequest request) {
        return roomService.update(id, request);
    }

    @Operation(summary = "Delete room")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(Roles.HAS_ADMIN)
    public void delete(@PathVariable Long id) {
        roomService.delete(id);
    }
}
