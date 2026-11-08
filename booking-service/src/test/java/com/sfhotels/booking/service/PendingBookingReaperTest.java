package com.sfhotels.booking.service;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.sfhotels.booking.entity.Booking;
import com.sfhotels.booking.entity.BookingStatus;
import com.sfhotels.booking.entity.Role;
import com.sfhotels.booking.entity.User;
import com.sfhotels.booking.support.IntegrationTest;
import java.sql.Timestamp;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class PendingBookingReaperTest extends IntegrationTest {

    @Autowired
    private PendingBookingReaper reaper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void stalePendingBookingIsCompensated() {
        User user = givenUser("user", Role.USER);
        Booking stale = givenBooking(user, 1L, BookingStatus.PENDING);
        Booking fresh = givenBooking(user, 2L, BookingStatus.PENDING);
        makeOlderThanTimeout(stale);
        hotelReleases(1L);

        reaper.cancelStalePending();

        assertThat(bookingRepository.findById(stale.getId()).orElseThrow().getStatus())
                .isEqualTo(BookingStatus.CANCELLED);
        assertThat(bookingRepository.findById(fresh.getId()).orElseThrow().getStatus())
                .isEqualTo(BookingStatus.PENDING);
        HOTEL_SERVICE.verify(1, postRequestedFor(urlEqualTo(releasePath(1L))));
    }

    private void makeOlderThanTimeout(Booking booking) {
        Timestamp longAgo = Timestamp.from(Instant.now().minusSeconds(3600));
        jdbcTemplate.update("update bookings set created_at = ? where id = ?", longAgo, booking.getId());
    }
}
