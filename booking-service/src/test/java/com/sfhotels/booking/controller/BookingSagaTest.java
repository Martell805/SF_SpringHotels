package com.sfhotels.booking.controller;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sfhotels.booking.dto.BookingRequest;
import com.sfhotels.booking.entity.BookingStatus;
import com.sfhotels.booking.entity.Role;
import com.sfhotels.booking.entity.User;
import com.sfhotels.booking.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class BookingSagaTest extends IntegrationTest {

    private static final long ROOM_ID = 1L;

    private User user;

    @BeforeEach
    void givenUserAndReleaseStub() {
        user = givenUser("user", Role.USER);
        hotelReleases(ROOM_ID);
    }

    @Test
    void successfulConfirmationMovesBookingToConfirmed() throws Exception {
        hotelConfirms(ROOM_ID, reservationOk());

        book(forRoom(ROOM_ID, "req-success"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.roomId").value(ROOM_ID));

        HOTEL_SERVICE.verify(1, postRequestedFor(urlEqualTo(confirmPath(ROOM_ID)))
                .withHeader(HttpHeaders.AUTHORIZATION, matching("Bearer .+"))
                .withHeader("traceparent", matching(".+"))
                .withRequestBody(matchingJsonPath("$.requestId", equalTo("req-success"))));
        HOTEL_SERVICE.verify(0, postRequestedFor(urlEqualTo(releasePath(ROOM_ID))));
    }

    @Test
    void remoteServerErrorIsRetriedThenCompensated() throws Exception {
        hotelConfirms(ROOM_ID, serverError());

        book(forRoom(ROOM_ID, "req-error"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503));

        HOTEL_SERVICE.verify(3, postRequestedFor(urlEqualTo(confirmPath(ROOM_ID))));
        HOTEL_SERVICE.verify(1, postRequestedFor(urlEqualTo(releasePath(ROOM_ID)))
                .withRequestBody(matchingJsonPath("$.requestId", equalTo("req-error"))));
        assertThat(statusOf("req-error")).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void remoteTimeoutIsRetriedThenCompensated() throws Exception {
        hotelConfirms(ROOM_ID, reservationOk().withFixedDelay(1_500));

        book(forRoom(ROOM_ID, "req-timeout")).andExpect(status().isServiceUnavailable());

        HOTEL_SERVICE.verify(3, postRequestedFor(urlEqualTo(confirmPath(ROOM_ID))));
        HOTEL_SERVICE.verify(1, postRequestedFor(urlEqualTo(releasePath(ROOM_ID))));
        assertThat(statusOf("req-timeout")).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void transientFailureRecoversOnRetryWithSameRequestId() throws Exception {
        hotelFailsOnceThenConfirms(ROOM_ID);

        book(forRoom(ROOM_ID, "req-flaky"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        HOTEL_SERVICE.verify(2, postRequestedFor(urlEqualTo(confirmPath(ROOM_ID)))
                .withRequestBody(matchingJsonPath("$.requestId", equalTo("req-flaky"))));
    }

    @Test
    void occupiedRoomIsConflictWithoutRetriesOrRelease() throws Exception {
        hotelConfirms(ROOM_ID, conflict());

        book(forRoom(ROOM_ID, "req-busy")).andExpect(status().isConflict());

        HOTEL_SERVICE.verify(1, postRequestedFor(urlEqualTo(confirmPath(ROOM_ID))));
        HOTEL_SERVICE.verify(0, postRequestedFor(urlEqualTo(releasePath(ROOM_ID))));
        assertThat(statusOf("req-busy")).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void redeliveredRequestReturnsSameBookingWithoutSecondReservation() throws Exception {
        hotelConfirms(ROOM_ID, reservationOk());

        String first = book(forRoom(ROOM_ID, "req-twice")).andReturn().getResponse().getContentAsString();
        String second = book(forRoom(ROOM_ID, "req-twice")).andReturn().getResponse().getContentAsString();

        assertThat(objectMapper.readTree(second).get("id")).isEqualTo(objectMapper.readTree(first).get("id"));
        assertThat(bookingRepository.count()).isEqualTo(1);
        HOTEL_SERVICE.verify(1, postRequestedFor(urlEqualTo(confirmPath(ROOM_ID))));
    }

    @Test
    void autoSelectSkipsOccupiedRoomAndTakesNextLeastBooked() throws Exception {
        hotelRecommends("[{\"id\": 3, \"timesBooked\": 0}, {\"id\": 1, \"timesBooked\": 2}]");
        hotelConfirms(3L, conflict());
        hotelConfirms(ROOM_ID, reservationOk());

        book(new BookingRequest(null, null, true, START, END, "req-auto"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roomId").value(ROOM_ID))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void autoSelectWithoutFreeRoomsIsConflict() throws Exception {
        hotelRecommends("[]");

        book(new BookingRequest(null, null, true, START, END, "req-none")).andExpect(status().isConflict());

        assertThat(bookingRepository.count()).isZero();
    }

    @Test
    void openCircuitFailsFastAndCompensates() throws Exception {
        hotelConfirms(ROOM_ID, serverError());
        for (int attempt = 0; attempt < 2; attempt++) {
            book(forRoom(ROOM_ID, "req-cb-" + attempt)).andExpect(status().isServiceUnavailable());
        }
        HOTEL_SERVICE.resetRequests();

        book(forRoom(ROOM_ID, "req-cb-open"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value(containsString("circuit open")));

        HOTEL_SERVICE.verify(0, postRequestedFor(urlEqualTo(confirmPath(ROOM_ID))));
        HOTEL_SERVICE.verify(1, postRequestedFor(urlEqualTo(releasePath(ROOM_ID))));
        assertThat(statusOf("req-cb-open")).isEqualTo(BookingStatus.CANCELLED);
    }

    private ResultActions book(BookingRequest request) throws Exception {
        return mockMvc.perform(post("/api/booking").header(HttpHeaders.AUTHORIZATION, bearer(user))
                .contentType(MediaType.APPLICATION_JSON).content(json(request)));
    }

    private BookingRequest forRoom(Long roomId, String requestId) {
        return new BookingRequest(roomId, null, false, START, END, requestId);
    }

    private BookingStatus statusOf(String requestId) {
        return bookingRepository.findByRequestId(requestId).orElseThrow().getStatus();
    }
}
