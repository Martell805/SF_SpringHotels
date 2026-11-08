package com.sfhotels.hotel.controller;

import com.sfhotels.common.security.Roles;
import com.sfhotels.common.web.PageResponse;
import com.sfhotels.hotel.dto.HotelDto;
import com.sfhotels.hotel.dto.HotelRequest;
import com.sfhotels.hotel.dto.HotelStatsDto;
import com.sfhotels.hotel.service.HotelService;
import com.sfhotels.hotel.service.StatisticsService;
import com.sfhotels.hotel.service.StatsPeriod;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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

@Tag(name = "Hotels")
@RestController
@RequestMapping("/api/hotels")
@RequiredArgsConstructor
public class HotelController {

    private final HotelService hotelService;
    private final StatisticsService statisticsService;

    @Operation(summary = "List hotels with optional name/city filters, paging and sorting")
    @GetMapping
    @PreAuthorize(Roles.HAS_USER_OR_ADMIN)
    public PageResponse<HotelDto> search(@RequestParam(required = false) String name,
                                         @RequestParam(required = false) String city,
                                         @ParameterObject @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return hotelService.search(name, city, pageable);
    }

    @Operation(summary = "Get hotel by id")
    @GetMapping("/{id}")
    @PreAuthorize(Roles.HAS_USER_OR_ADMIN)
    public HotelDto get(@PathVariable Long id) {
        return hotelService.get(id);
    }

    @Operation(summary = "Occupancy statistics per hotel for the period (default: next 30 days)")
    @GetMapping("/stats")
    @PreAuthorize(Roles.HAS_ADMIN)
    public List<HotelStatsDto> stats(@RequestParam(required = false) LocalDate from,
                                     @RequestParam(required = false) LocalDate to) {
        return statisticsService.hotelStats(StatsPeriod.of(from, to));
    }

    @Operation(summary = "Create hotel")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Roles.HAS_ADMIN)
    public HotelDto create(@Valid @RequestBody HotelRequest request) {
        return hotelService.create(request);
    }

    @Operation(summary = "Update hotel")
    @PutMapping("/{id}")
    @PreAuthorize(Roles.HAS_ADMIN)
    public HotelDto update(@PathVariable Long id, @Valid @RequestBody HotelRequest request) {
        return hotelService.update(id, request);
    }

    @Operation(summary = "Delete hotel with its rooms")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(Roles.HAS_ADMIN)
    public void delete(@PathVariable Long id) {
        hotelService.delete(id);
    }
}
