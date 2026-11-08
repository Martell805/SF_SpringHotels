package com.sfhotels.booking.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.sfhotels.booking.dto.AuthRequest;
import com.sfhotels.booking.support.IntegrationTest;
import com.sfhotels.common.security.SecurityProperties;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.ResultActions;

class AuthControllerTest extends IntegrationTest {

    @Autowired
    private SecurityProperties securityProperties;

    @Test
    void registerReturnsOneHourToken() throws Exception {
        register("newcomer", "secret123")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600));
    }

    @Test
    void registerWithTakenUsernameIsConflict() throws Exception {
        register("newcomer", "secret123").andExpect(status().isCreated());

        register("newcomer", "another123").andExpect(status().isConflict());
    }

    @Test
    void registerWithShortPasswordIsBadRequest() throws Exception {
        register("newcomer", "123")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.password").exists());
    }

    @Test
    void authenticateWithValidCredentialsReturnsToken() throws Exception {
        register("newcomer", "secret123");

        authenticate("newcomer", "secret123")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void authenticateWithWrongPasswordIsUnauthorized() throws Exception {
        register("newcomer", "secret123");

        authenticate("newcomer", "wrong-password")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void registeredTokenGrantsAccessToOwnHistory() throws Exception {
        String body = register("newcomer", "secret123").andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(body).get("accessToken").asText();

        mockMvc.perform(get("/api/bookings").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void requestWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/bookings"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void malformedTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/bookings").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/bookings").header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken()))
                .andExpect(status().isUnauthorized());
    }

    private ResultActions register(String username, String password) throws Exception {
        return mockMvc.perform(post("/api/user/register").contentType(MediaType.APPLICATION_JSON)
                .content(json(new AuthRequest(username, password))));
    }

    private ResultActions authenticate(String username, String password) throws Exception {
        return mockMvc.perform(post("/api/user/auth").contentType(MediaType.APPLICATION_JSON)
                .content(json(new AuthRequest(username, password))));
    }

    private String expiredToken() {
        Instant issued = Instant.now().minusSeconds(7200);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject("ghost")
                .issuedAt(issued)
                .expiresAt(issued.plusSeconds(3600))
                .claim("uid", 1L)
                .claim("roles", List.of("USER"))
                .build();
        NimbusJwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(securityProperties.secretKey()));
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }
}
