package com.sfhotels.hotel.repository;

import com.sfhotels.hotel.entity.Hotel;
import org.springframework.data.jpa.domain.Specification;

public final class HotelSpecifications {

    private HotelSpecifications() {
    }

    public static Specification<Hotel> nameContains(String name) {
        return containsIgnoreCase("name", name);
    }

    public static Specification<Hotel> cityContains(String city) {
        return containsIgnoreCase("city", city);
    }

    private static Specification<Hotel> containsIgnoreCase(String attribute, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String pattern = "%" + value.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get(attribute)), pattern);
    }
}
