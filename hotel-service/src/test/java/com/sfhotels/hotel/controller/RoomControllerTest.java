package com.sfhotels.hotel.controller;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sfhotels.hotel.dto.ReservationRequest;
import com.sfhotels.hotel.dto.RoomRequest;
import com.sfhotels.hotel.entity.Hotel;
import com.sfhotels.hotel.entity.Room;
import com.sfhotels.hotel.service.ReservationService;
import com.sfhotels.hotel.support.IntegrationTest;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

class RoomControllerTest extends IntegrationTest {

    private static final LocalDate START = LocalDate.now().plusDays(10);
    private static final LocalDate END = START.plusDays(3);

    @Autowired
    private ReservationService reservationService;

    @Test
    void adminAddsRoomToHotel() throws Exception {
        Hotel hotel = givenHotel("Neva", "Saint Petersburg");
        RoomRequest request = new RoomRequest(hotel.getId(), "301", 2, new BigDecimal("6500"), null);

        mockMvc.perform(post("/api/rooms").with(asAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content(json(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.hotelId").value(hotel.getId()))
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.timesBooked").value(0));
    }

    @Test
    void duplicateRoomNumberInHotelIsConflict() throws Exception {
        Hotel hotel = givenHotel("Neva", "Saint Petersburg");
        givenRoom(hotel, "301", 0, true);
        RoomRequest request = new RoomRequest(hotel.getId(), "301", 2, new BigDecimal("6500"), true);

        mockMvc.perform(post("/api/rooms").with(asAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content(json(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void freeRoomsExcludeOccupiedAndOutOfServiceRooms() throws Exception {
        Hotel hotel = givenHotel("Neva", "Saint Petersburg");
        Room free = givenRoom(hotel, "101", 0, true);
        Room occupied = givenRoom(hotel, "102", 0, true);
        givenRoom(hotel, "103", 0, false);
        reservationService.confirm(occupied.getId(), new ReservationRequest("req-1", 1L, START, END));

        mockMvc.perform(get("/api/rooms").with(asUser())
                        .param("startDate", START.toString()).param("endDate", END.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", contains(free.getId().intValue())));
    }

    @Test
    void recommendSortsByTimesBookedThenById() throws Exception {
        Hotel hotel = givenHotel("Neva", "Saint Petersburg");
        Room busy = givenRoom(hotel, "101", 5, true);
        Room quietFirst = givenRoom(hotel, "102", 1, true);
        Room quietSecond = givenRoom(hotel, "103", 1, true);
        Room idle = givenRoom(hotel, "104", 0, true);

        mockMvc.perform(get("/api/rooms/recommend").with(asUser())
                        .param("startDate", START.toString()).param("endDate", END.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", contains(idle.getId().intValue(), quietFirst.getId().intValue(),
                        quietSecond.getId().intValue(), busy.getId().intValue())));
    }

    @Test
    void searchWithOnlyOneDateIsBadRequest() throws Exception {
        mockMvc.perform(get("/api/rooms").with(asUser()).param("startDate", START.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void userCannotCallInternalConfirmEndpoint() throws Exception {
        Room room = givenRoom(givenHotel("Neva", "Saint Petersburg"), "101", 0, true);

        mockMvc.perform(post("/api/rooms/{id}/confirm-availability", room.getId()).with(asUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new ReservationRequest("req-1", 1L, START, END))))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminSeesRoomStatsWithBookedNights() throws Exception {
        Room room = givenRoom(givenHotel("Neva", "Saint Petersburg"), "101", 0, true);
        reservationService.confirm(room.getId(), new ReservationRequest("req-1", 1L, START, END));

        mockMvc.perform(get("/api/rooms/stats").with(asAdmin())
                        .param("from", START.toString()).param("to", START.plusDays(6).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].timesBooked").value(1))
                .andExpect(jsonPath("$[0].bookedNights").value(3))
                .andExpect(jsonPath("$[0].occupancyPercent").value(50.0));
    }
}
