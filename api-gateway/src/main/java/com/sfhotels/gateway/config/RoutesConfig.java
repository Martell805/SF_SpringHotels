package com.sfhotels.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RoutesConfig {

    static final String[] INTERNAL_PATHS = {"/api/rooms/*/confirm-availability", "/api/rooms/*/release"};

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("booking-service", route -> route
                        .path("/api/bookings/**", "/api/booking/**", "/api/user/**")
                        .uri("lb://booking-service"))
                .route("hotel-service", route -> route
                        .path("/api/hotels/**", "/api/rooms/**")
                        .and().not(internal -> internal.path(INTERNAL_PATHS))
                        .uri("lb://hotel-service"))
                .build();
    }
}
