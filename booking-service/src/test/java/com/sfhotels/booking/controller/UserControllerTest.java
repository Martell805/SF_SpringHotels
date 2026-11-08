package com.sfhotels.booking.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sfhotels.booking.dto.UserCreateRequest;
import com.sfhotels.booking.dto.UserUpdateRequest;
import com.sfhotels.booking.entity.BookingStatus;
import com.sfhotels.booking.entity.Role;
import com.sfhotels.booking.entity.User;
import com.sfhotels.booking.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class UserControllerTest extends IntegrationTest {

    private User admin;
    private User user;

    @BeforeEach
    void givenUsers() {
        admin = givenUser("admin", Role.ADMIN);
        user = givenUser("user", Role.USER);
    }

    @Test
    void userCannotListUsers() throws Exception {
        mockMvc.perform(get("/api/user").header(HttpHeaders.AUTHORIZATION, bearer(user)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void adminListsUsers() throws Exception {
        mockMvc.perform(get("/api/user").header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].password").doesNotExist());
    }

    @Test
    void adminCreatesAdmin() throws Exception {
        UserCreateRequest request = new UserCreateRequest("second-admin", "secret123", Role.ADMIN);

        mockMvc.perform(post("/api/user").header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(json(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void adminPromotesUser() throws Exception {
        mockMvc.perform(patch("/api/user/{id}", user.getId()).header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(json(new UserUpdateRequest(null, null,
                                Role.ADMIN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("user"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void adminDeletesUserWithoutBookings() throws Exception {
        mockMvc.perform(delete("/api/user/{id}", user.getId()).header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/user/{id}", user.getId()).header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isNotFound());
    }

    @Test
    void userWithBookingsCannotBeDeleted() throws Exception {
        givenBooking(user, 1L, BookingStatus.CONFIRMED);

        mockMvc.perform(delete("/api/user/{id}", user.getId()).header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isConflict());
    }
}
