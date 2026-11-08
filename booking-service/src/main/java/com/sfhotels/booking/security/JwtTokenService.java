package com.sfhotels.booking.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.sfhotels.booking.dto.TokenResponse;
import com.sfhotels.booking.entity.User;
import com.sfhotels.common.security.Roles;
import com.sfhotels.common.security.SecurityProperties;
import com.sfhotels.common.security.TokenClaims;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService implements TokenService {

    private static final Duration SERVICE_TOKEN_TTL = Duration.ofMinutes(5);

    private final JwtEncoder encoder;
    private final Duration userTokenTtl;
    private final String issuer;

    public JwtTokenService(SecurityProperties properties, @Value("${spring.application.name}") String issuer) {
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(properties.secretKey()));
        this.userTokenTtl = properties.tokenTtl();
        this.issuer = issuer;
    }

    @Override
    public TokenResponse issueUserToken(User user) {
        Map<String, Object> claims = Map.of(
                TokenClaims.USER_ID, user.getId(),
                TokenClaims.ROLES, List.of(user.getRole().name()));
        return TokenResponse.bearer(encode(user.getUsername(), claims, userTokenTtl), userTokenTtl.toSeconds());
    }

    @Override
    public String issueServiceToken() {
        return encode(issuer, Map.of(TokenClaims.ROLES, List.of(Roles.INTERNAL)), SERVICE_TOKEN_TTL);
    }

    private String encode(String subject, Map<String, Object> claims, Duration ttl) {
        Instant now = Instant.now();
        JwtClaimsSet claimsSet = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(subject)
                .issuedAt(now)
                .expiresAt(now.plus(ttl))
                .claims(target -> target.putAll(claims))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claimsSet)).getTokenValue();
    }
}
