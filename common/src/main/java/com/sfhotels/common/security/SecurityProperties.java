package com.sfhotels.common.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("app.security")
public record SecurityProperties(
        String jwtSecret,
        @DefaultValue("PT1H") Duration tokenTtl,
        @DefaultValue List<String> publicPaths) {

    public SecretKey secretKey() {
        return new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }
}
