package com.sfhotels.booking.config;

import com.sfhotels.booking.entity.Booking;
import com.sfhotels.booking.entity.BookingStatus;
import com.sfhotels.booking.entity.Role;
import com.sfhotels.booking.entity.User;
import com.sfhotels.booking.repository.BookingRepository;
import com.sfhotels.booking.repository.UserRepository;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.demo-data.enabled", havingValue = "true")
public class DemoDataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        user("admin", "admin123", Role.ADMIN);
        User user = user("user", "user123", Role.USER);
        User alice = user("alice", "alice123", Role.USER);
        user("bob", "bob12345", Role.USER);

        LocalDate today = LocalDate.now();
        booking(user, 1L, today.plusDays(5), today.plusDays(8), BookingStatus.CONFIRMED, "seed-booking-1");
        booking(user, 5L, today.plusDays(10), today.plusDays(12), BookingStatus.CONFIRMED, "seed-booking-2");
        booking(alice, 6L, today.plusDays(3), today.plusDays(4), BookingStatus.CANCELLED, "seed-booking-3");
        log.info("Demo data loaded: {} users, {} bookings", userRepository.count(), bookingRepository.count());
    }

    private User user(String username, String password, Role role) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        return userRepository.save(user);
    }

    private void booking(User user, Long roomId, LocalDate start, LocalDate end, BookingStatus status,
                         String requestId) {
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setRoomId(roomId);
        booking.setStartDate(start);
        booking.setEndDate(end);
        booking.setStatus(status);
        booking.setRequestId(requestId);
        bookingRepository.save(booking);
    }
}
