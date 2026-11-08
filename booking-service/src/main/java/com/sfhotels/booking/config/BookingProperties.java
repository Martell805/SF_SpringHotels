package com.sfhotels.booking.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("app.booking")
public record BookingProperties(
        @DefaultValue("3") int autoSelectAttempts,
        @DefaultValue("PT2M") Duration pendingTimeout) {
}
