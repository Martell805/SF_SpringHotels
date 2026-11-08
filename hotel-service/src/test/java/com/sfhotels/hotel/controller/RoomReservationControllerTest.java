package com.sfhotels.hotel.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sfhotels.hotel.dto.ReleaseRequest;
import com.sfhotels.hotel.dto.ReservationRequest;
import com.sfhotels.hotel.entity.Room;
import com.sfhotels.hotel.support.IntegrationTest;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class RoomReservationControllerTest extends IntegrationTest {

    private static final LocalDate START = LocalDate.now().plusDays(10);
    private static final LocalDate END = START.plusDays(3);

    private Room room;

    @BeforeEach
    void givenRoom() {
        room = givenRoom(givenHotel("Neva", "Saint Petersburg"), "101", 0, true);
    }

    @Test
    void confirmLocksSlotAndIncrementsTimesBooked() throws Exception {
        confirm("req-1", START, END)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        assertThat(timesBooked()).isEqualTo(1);
    }

    @Test
    void repeatedConfirmWithSameRequestIdIsIdempotent() throws Exception {
        confirm("req-1", START, END).andExpect(status().isOk());

        confirm("req-1", START, END).andExpect(status().isOk());

        assertThat(reservationRepository.count()).isEqualTo(1);
        assertThat(timesBooked()).isEqualTo(1);
    }

    @Test
    void overlappingDatesAreRejectedWithConflict() throws Exception {
        confirm("req-1", START, END).andExpect(status().isOk());

        confirm("req-2", START.plusDays(2), END.plusDays(2))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void adjacentDatesDoNotOverlap() throws Exception {
        confirm("req-1", START, END).andExpect(status().isOk());

        confirm("req-2", END, END.plusDays(2)).andExpect(status().isOk());
    }

    @Test
    void releaseFreesSlotAndIsIdempotent() throws Exception {
        confirm("req-1", START, END).andExpect(status().isOk());

        release("req-1").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("RELEASED"));
        release("req-1").andExpect(status().isOk());

        assertThat(timesBooked()).isZero();
        confirm("req-2", START, END).andExpect(status().isOk());
    }

    @Test
    void lateConfirmAfterCompensatingReleaseIsRejected() throws Exception {
        release("req-1").andExpect(status().isOk());

        confirm("req-1", START, END).andExpect(status().isConflict());

        assertThat(timesBooked()).isZero();
    }

    @Test
    void invalidDateRangeIsBadRequest() throws Exception {
        confirm("req-1", END, START)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.dateRangeValid").exists());
    }

    @Test
    void unknownRoomIsNotFound() throws Exception {
        mockMvc.perform(post("/api/rooms/{id}/confirm-availability", 999_999).with(asInternal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new ReservationRequest("req-1", 1L, START, END))))
                .andExpect(status().isNotFound());
    }

    private ResultActions confirm(String requestId, LocalDate start, LocalDate end) throws Exception {
        return mockMvc.perform(post("/api/rooms/{id}/confirm-availability", room.getId()).with(asInternal())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(new ReservationRequest(requestId, 1L, start, end))));
    }

    private ResultActions release(String requestId) throws Exception {
        return mockMvc.perform(post("/api/rooms/{id}/release", room.getId()).with(asInternal())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(new ReleaseRequest(requestId, 1L))));
    }

    private long timesBooked() {
        return roomRepository.findById(room.getId()).orElseThrow().getTimesBooked();
    }
}
