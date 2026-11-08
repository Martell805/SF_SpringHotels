package com.sfhotels.booking.repository;

import com.sfhotels.booking.entity.Booking;
import com.sfhotels.booking.entity.BookingStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByRequestId(String requestId);

    Page<Booking> findByUserId(Long userId, Pageable pageable);

    boolean existsByUserId(Long userId);

    List<Booking> findByStatusAndCreatedAtBefore(BookingStatus status, Instant createdBefore);
}
