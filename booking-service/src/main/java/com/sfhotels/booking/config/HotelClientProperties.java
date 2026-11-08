package com.sfhotels.booking.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("app.hotel-client")
public record HotelClientProperties(
        @DefaultValue("http://hotel-service") String baseUrl,
        @DefaultValue("1s") Duration connectTimeout,
        @DefaultValue("3s") Duration readTimeout) {
}
