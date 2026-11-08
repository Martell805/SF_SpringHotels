package com.sfhotels.hotel.repository;

import com.sfhotels.hotel.entity.ReservationStatus;
import com.sfhotels.hotel.entity.Room;
import com.sfhotels.hotel.entity.RoomReservation;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.time.LocalDate;
import org.springframework.data.jpa.domain.Specification;

public final class RoomSpecifications {

    private RoomSpecifications() {
    }

    public static Specification<Room> operational() {
        return (root, query, cb) -> cb.isTrue(root.get("available"));
    }

    public static Specification<Room> inHotel(Long hotelId) {
        return hotelId == null ? null : (root, query, cb) -> cb.equal(root.get("hotel").get("id"), hotelId);
    }

    public static Specification<Room> minCapacity(Integer capacity) {
        return capacity == null ? null : (root, query, cb) -> cb.ge(root.get("capacity"), capacity);
    }

    public static Specification<Room> freeBetween(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return null;
        }
        return (root, query, cb) -> {
            Subquery<Long> overlapping = query.subquery(Long.class);
            Root<RoomReservation> reservation = overlapping.from(RoomReservation.class);
            overlapping.select(reservation.get("id")).where(
                    cb.equal(reservation.get("room"), root),
                    cb.equal(reservation.get("status"), ReservationStatus.ACTIVE),
                    cb.lessThan(reservation.get("startDate"), endDate),
                    cb.greaterThan(reservation.get("endDate"), startDate));
            return cb.not(cb.exists(overlapping));
        };
    }
}
