package com.sfhotels.hotel.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sfhotels.hotel.dto.HotelRequest;
import com.sfhotels.hotel.entity.Hotel;
import com.sfhotels.hotel.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class HotelControllerTest extends IntegrationTest {

    @Test
    void adminCreatesHotel() throws Exception {
        HotelRequest request = new HotelRequest("Sea View", "Beach road, 7", "Sochi");

        mockMvc.perform(post("/api/hotels").with(asAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content(json(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Sea View"));
    }

    @Test
    void userCannotCreateHotel() throws Exception {
        HotelRequest request = new HotelRequest("Sea View", "Beach road, 7", "Sochi");

        mockMvc.perform(post("/api/hotels").with(asUser())
                        .contentType(MediaType.APPLICATION_JSON).content(json(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void anonymousRequestIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/hotels"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/api/hotels"));
    }

    @Test
    void invalidHotelIsRejectedWithFieldDetails() throws Exception {
        HotelRequest request = new HotelRequest("", "Beach road, 7", "Sochi");

        mockMvc.perform(post("/api/hotels").with(asAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content(json(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.name").exists());
    }

    @Test
    void userSearchesHotelsByCityWithPaging() throws Exception {
        givenHotel("Neva", "Saint Petersburg");
        givenHotel("Central", "Moscow");
        givenHotel("Arbat", "Moscow");

        mockMvc.perform(get("/api/hotels").with(asUser())
                        .param("city", "moscow").param("size", "1").param("sort", "name,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("Arbat"))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void adminUpdatesAndDeletesHotel() throws Exception {
        Hotel hotel = givenHotel("Old name", "Kazan");
        HotelRequest request = new HotelRequest("New name", "Kremlevskaya, 1", "Kazan");

        mockMvc.perform(put("/api/hotels/{id}", hotel.getId()).with(asAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content(json(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New name"));
        mockMvc.perform(delete("/api/hotels/{id}", hotel.getId()).with(asAdmin()))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/hotels/{id}", hotel.getId()).with(asUser()))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminSeesHotelOccupancyStats() throws Exception {
        Hotel hotel = givenHotel("Neva", "Saint Petersburg");
        givenRoom(hotel, "101", 4, true);
        givenRoom(hotel, "102", 1, true);

        mockMvc.perform(get("/api/hotels/stats").with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].rooms").value(2))
                .andExpect(jsonPath("$[0].totalTimesBooked").value(5));
    }

    @Test
    void invalidStatsPeriodIsBadRequest() throws Exception {
        mockMvc.perform(get("/api/hotels/stats").with(asAdmin())
                        .param("from", "2030-01-10").param("to", "2030-01-01"))
                .andExpect(status().isBadRequest());
    }
}
