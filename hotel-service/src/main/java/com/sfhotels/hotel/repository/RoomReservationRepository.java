package com.sfhotels.hotel.repository;

import com.sfhotels.hotel.entity.ReservationStatus;
import com.sfhotels.hotel.entity.RoomReservation;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomReservationRepository extends JpaRepository<RoomReservation, Long> {

    Optional<RoomReservation> findByRequestId(String requestId);

    @Query("""
            select count(r) > 0 from RoomReservation r
            where r.room.id = :roomId and r.status = :status
              and r.startDate < :endDate and r.endDate > :startDate
            """)
    boolean existsOverlapping(@Param("roomId") Long roomId,
                              @Param("status") ReservationStatus status,
                              @Param("startDate") LocalDate startDate,
                              @Param("endDate") LocalDate endDate);

    @Query("""
            select r from RoomReservation r join fetch r.room
            where r.status = :status and r.startDate < :to and r.endDate > :from
            """)
    List<RoomReservation> findOverlappingPeriod(@Param("status") ReservationStatus status,
                                                @Param("from") LocalDate from,
                                                @Param("to") LocalDate to);
}
