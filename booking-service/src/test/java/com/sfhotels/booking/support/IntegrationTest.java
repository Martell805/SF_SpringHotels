package com.sfhotels.booking.support;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.sfhotels.booking.entity.Booking;
import com.sfhotels.booking.entity.BookingStatus;
import com.sfhotels.booking.entity.Role;
import com.sfhotels.booking.entity.User;
import com.sfhotels.booking.repository.BookingRepository;
import com.sfhotels.booking.repository.UserRepository;
import com.sfhotels.booking.security.TokenService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureObservability(metrics = false)
@ActiveProfiles("test")
public abstract class IntegrationTest {

    protected static final WireMockServer HOTEL_SERVICE = new WireMockServer(options().dynamicPort());
    protected static final LocalDate START = LocalDate.now().plusDays(7);
    protected static final LocalDate END = START.plusDays(3);

    static {
        HOTEL_SERVICE.start();
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected BookingRepository bookingRepository;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @DynamicPropertySource
    static void hotelServiceInstance(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.discovery.client.simple.instances.hotel-service[0].uri", HOTEL_SERVICE::baseUrl);
    }

    @BeforeEach
    void resetState() {
        HOTEL_SERVICE.resetAll();
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(CircuitBreaker::reset);
        bookingRepository.deleteAll();
        userRepository.deleteAll();
    }

    protected User givenUser(String username, Role role) {
        User user = new User();
        user.setUsername(username);
        user.setPassword("{noop}unused");
        user.setRole(role);
        return userRepository.save(user);
    }

    protected Booking givenBooking(User user, Long roomId, BookingStatus status) {
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setRoomId(roomId);
        booking.setStartDate(START);
        booking.setEndDate(END);
        booking.setStatus(status);
        booking.setRequestId(UUID.randomUUID().toString());
        return bookingRepository.save(booking);
    }

    protected String bearer(User user) {
        return "Bearer " + tokenService.issueUserToken(user).accessToken();
    }

    protected String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    protected static void hotelConfirms(Long roomId, ResponseDefinitionBuilder response) {
        HOTEL_SERVICE.stubFor(post(urlEqualTo(confirmPath(roomId))).willReturn(response));
    }

    protected static void hotelReleases(Long roomId) {
        HOTEL_SERVICE.stubFor(post(urlEqualTo(releasePath(roomId))).willReturn(okJson("{\"status\":\"RELEASED\"}")));
    }

    protected static void hotelRecommends(String roomsJson) {
        HOTEL_SERVICE.stubFor(get(urlPathEqualTo("/api/rooms/recommend")).willReturn(okJson(roomsJson)));
    }

    protected static void hotelFailsOnceThenConfirms(Long roomId) {
        HOTEL_SERVICE.stubFor(post(urlEqualTo(confirmPath(roomId))).inScenario("flaky")
                .whenScenarioStateIs(Scenario.STARTED).willReturn(serverError()).willSetStateTo("recovered"));
        HOTEL_SERVICE.stubFor(post(urlEqualTo(confirmPath(roomId))).inScenario("flaky")
                .whenScenarioStateIs("recovered").willReturn(reservationOk()));
    }

    protected static void hotelReleaseFails(Long roomId) {
        HOTEL_SERVICE.stubFor(post(urlEqualTo(releasePath(roomId))).willReturn(serverError()));
    }

    protected static ResponseDefinitionBuilder reservationOk() {
        return okJson("{\"status\":\"ACTIVE\"}");
    }

    protected static ResponseDefinitionBuilder conflict() {
        return aResponse().withStatus(409).withHeader("Content-Type", "application/json")
                .withBody("{\"message\":\"Room is already booked for the requested dates\"}");
    }

    protected static ResponseDefinitionBuilder serverError() {
        return aResponse().withStatus(500);
    }

    protected static String confirmPath(Long roomId) {
        return "/api/rooms/" + roomId + "/confirm-availability";
    }

    protected static String releasePath(Long roomId) {
        return "/api/rooms/" + roomId + "/release";
    }
}
