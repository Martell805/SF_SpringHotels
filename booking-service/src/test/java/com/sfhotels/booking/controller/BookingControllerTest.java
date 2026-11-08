package com.sfhotels.booking.controller;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sfhotels.booking.dto.BookingRequest;
import com.sfhotels.booking.entity.Booking;
import com.sfhotels.booking.entity.BookingStatus;
import com.sfhotels.booking.entity.Role;
import com.sfhotels.booking.entity.User;
import com.sfhotels.booking.support.IntegrationTest;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class BookingControllerTest extends IntegrationTest {

    private User owner;
    private User stranger;
    private User admin;

    @BeforeEach
    void givenUsers() {
        owner = givenUser("owner", Role.USER);
        stranger = givenUser("stranger", Role.USER);
        admin = givenUser("admin", Role.ADMIN);
    }

    @Test
    void historyContainsOnlyOwnBookingsWithPaging() throws Exception {
        givenBooking(owner, 1L, BookingStatus.CONFIRMED);
        givenBooking(owner, 2L, BookingStatus.CANCELLED);
        givenBooking(stranger, 3L, BookingStatus.CONFIRMED);

        mockMvc.perform(get("/api/bookings").header(HttpHeaders.AUTHORIZATION, bearer(owner)).param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].userId").value(owner.getId()))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void ownerReadsOwnBooking() throws Exception {
        Booking booking = givenBooking(owner, 1L, BookingStatus.CONFIRMED);

        mockMvc.perform(get("/api/booking/{id}", booking.getId()).header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(booking.getId()));
    }

    @Test
    void foreignBookingIsForbidden() throws Exception {
        Booking booking = givenBooking(owner, 1L, BookingStatus.CONFIRMED);

        mockMvc.perform(get("/api/booking/{id}", booking.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(stranger)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminReadsAnyBooking() throws Exception {
        Booking booking = givenBooking(owner, 1L, BookingStatus.CONFIRMED);

        mockMvc.perform(get("/api/booking/{id}", booking.getId()).header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk());
    }

    @Test
    void missingBookingIsNotFound() throws Exception {
        mockMvc.perform(get("/api/booking/{id}", 404_404).header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isNotFound());
    }

    @Test
    void cancelConfirmedBookingReleasesSlot() throws Exception {
        Booking booking = givenBooking(owner, 1L, BookingStatus.CONFIRMED);
        hotelReleases(1L);

        mockMvc.perform(delete("/api/booking/{id}", booking.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        HOTEL_SERVICE.verify(1, postRequestedFor(urlEqualTo(releasePath(1L))));
    }

    @Test
    void cancelKeepsBookingConfirmedWhenHotelIsDown() throws Exception {
        Booking booking = givenBooking(owner, 1L, BookingStatus.CONFIRMED);
        hotelReleaseFails(1L);

        mockMvc.perform(delete("/api/booking/{id}", booking.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isServiceUnavailable());

        mockMvc.perform(get("/api/booking/{id}", booking.getId()).header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void cancelOfForeignBookingIsForbidden() throws Exception {
        Booking booking = givenBooking(owner, 1L, BookingStatus.CONFIRMED);

        mockMvc.perform(delete("/api/booking/{id}", booking.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(stranger)))
                .andExpect(status().isForbidden());

        HOTEL_SERVICE.verify(0, postRequestedFor(urlEqualTo(releasePath(1L))));
    }

    @Test
    void endDateBeforeStartDateIsBadRequest() throws Exception {
        book(new BookingRequest(1L, null, false, END, START, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.dateRangeValid").exists());
    }

    @Test
    void pastStartDateIsBadRequest() throws Exception {
        book(new BookingRequest(1L, null, false, LocalDate.now().minusDays(1), END, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.startDate").exists());
    }

    @Test
    void tooLongStayIsBadRequest() throws Exception {
        book(new BookingRequest(1L, null, false, START, START.plusDays(31), null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.stayLengthValid").exists());
    }

    @Test
    void missingRoomWithoutAutoSelectIsBadRequest() throws Exception {
        book(new BookingRequest(null, null, false, START, END, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.roomSpecified").exists());
    }

    @Test
    void recommendationsAreProxiedInHotelOrder() throws Exception {
        hotelRecommends("[{\"id\": 7, \"timesBooked\": 0}, {\"id\": 2, \"timesBooked\": 3}]");

        mockMvc.perform(get("/api/bookings/recommendations").header(HttpHeaders.AUTHORIZATION, bearer(owner))
                        .param("startDate", START.toString()).param("endDate", END.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", contains(7, 2)));

        HOTEL_SERVICE.verify(getRequestedFor(urlPathEqualTo("/api/rooms/recommend"))
                .withQueryParam("startDate", equalTo(START.toString()))
                .withQueryParam("endDate", equalTo(END.toString())));
    }

    private ResultActions book(BookingRequest request) throws Exception {
        return mockMvc.perform(post("/api/booking").header(HttpHeaders.AUTHORIZATION, bearer(owner))
                .contentType(MediaType.APPLICATION_JSON).content(json(request)));
    }
}
