package com.sfhotels.hotel.support;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sfhotels.hotel.entity.Hotel;
import com.sfhotels.hotel.entity.Room;
import com.sfhotels.hotel.repository.HotelRepository;
import com.sfhotels.hotel.repository.RoomRepository;
import com.sfhotels.hotel.repository.RoomReservationRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected HotelRepository hotelRepository;

    @Autowired
    protected RoomRepository roomRepository;

    @Autowired
    protected RoomReservationRepository reservationRepository;

    @BeforeEach
    void cleanDatabase() {
        reservationRepository.deleteAll();
        roomRepository.deleteAll();
        hotelRepository.deleteAll();
    }

    protected static JwtRequestPostProcessor asUser() {
        return withRole("USER");
    }

    protected static JwtRequestPostProcessor asAdmin() {
        return withRole("ADMIN");
    }

    protected static JwtRequestPostProcessor asInternal() {
        return withRole("INTERNAL");
    }

    private static JwtRequestPostProcessor withRole(String role) {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    protected String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    protected Hotel givenHotel(String name, String city) {
        Hotel hotel = new Hotel();
        hotel.setName(name);
        hotel.setAddress(name + " street, 1");
        hotel.setCity(city);
        return hotelRepository.save(hotel);
    }

    protected Room givenRoom(Hotel hotel, String number, long timesBooked, boolean available) {
        Room room = new Room();
        room.setHotel(hotel);
        room.setNumber(number);
        room.setCapacity(2);
        room.setPricePerNight(new BigDecimal("5000"));
        room.setTimesBooked(timesBooked);
        room.setAvailable(available);
        return roomRepository.save(room);
    }
}
