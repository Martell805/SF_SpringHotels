package com.sfhotels.gateway;

import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.sfhotels.gateway.filter.RequestTracingFilter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "eureka.client.enabled=false")
@AutoConfigureObservability(metrics = false)
class GatewayRoutingTest {

    private static final WireMockServer BACKEND = new WireMockServer(options().dynamicPort());

    static {
        BACKEND.start();
    }

    @Autowired
    private WebTestClient webTestClient;

    @Value("${app.security.jwt-secret}")
    private String jwtSecret;

    @DynamicPropertySource
    static void backendInstances(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.discovery.client.simple.instances.booking-service[0].uri", BACKEND::baseUrl);
        registry.add("spring.cloud.discovery.client.simple.instances.hotel-service[0].uri", BACKEND::baseUrl);
    }

    @BeforeEach
    void resetBackend() {
        BACKEND.resetAll();
        BACKEND.stubFor(WireMock.any(anyUrl()).willReturn(okJson("{}")));
    }

    @Test
    void protectedRouteWithoutTokenIsUnauthorized() {
        webTestClient.get().uri("/api/hotels").exchange().expectStatus().isUnauthorized();

        BACKEND.verify(0, anyRequestedFor(anyUrl()));
    }

    @Test
    void invalidTokenIsUnauthorized() {
        webTestClient.get().uri("/api/hotels").header(HttpHeaders.AUTHORIZATION, "Bearer broken")
                .exchange().expectStatus().isUnauthorized();
    }

    @Test
    void publicAuthRouteIsProxiedWithoutToken() {
        webTestClient.post().uri("/api/user/auth").bodyValue("{}").header("Content-Type", "application/json")
                .exchange().expectStatus().isOk();

        BACKEND.verify(1, WireMock.postRequestedFor(urlEqualTo("/api/user/auth")));
    }

    @Test
    void hotelRouteRelaysTokenAndTraceContext() {
        String token = validToken();

        webTestClient.get().uri("/api/hotels?city=Moscow").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().exists(RequestTracingFilter.TRACE_HEADER);

        BACKEND.verify(1, getRequestedFor(urlEqualTo("/api/hotels?city=Moscow"))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer " + token))
                .withHeader("traceparent", matching(".+")));
    }

    @Test
    void bookingRoutesAreProxied() {
        webTestClient.get().uri("/api/bookings").header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken())
                .exchange().expectStatus().isOk();
        webTestClient.get().uri("/api/booking/1").header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken())
                .exchange().expectStatus().isOk();

        BACKEND.verify(1, getRequestedFor(urlEqualTo("/api/bookings")));
        BACKEND.verify(1, getRequestedFor(urlEqualTo("/api/booking/1")));
    }

    @Test
    void internalRoomEndpointsAreNotPublished() {
        String token = validToken();

        webTestClient.post().uri("/api/rooms/1/confirm-availability")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange().expectStatus().isNotFound();
        webTestClient.post().uri("/api/rooms/1/release")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange().expectStatus().isNotFound();

        BACKEND.verify(0, anyRequestedFor(anyUrl()));
    }

    private String validToken() {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject("user")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .claim("roles", List.of("USER"))
                .build();
        SecretKeySpec key = new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return new NimbusJwtEncoder(new ImmutableSecret<>(key))
                .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }
}
