package com.sfhotels.booking.service;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.sfhotels.booking.dto.BookingDto;
import com.sfhotels.booking.dto.BookingRequest;
import com.sfhotels.booking.entity.Role;
import com.sfhotels.booking.entity.User;
import com.sfhotels.booking.security.CurrentUser;
import com.sfhotels.booking.support.IntegrationTest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class BookingConcurrencyTest extends IntegrationTest {

    private static final int THREADS = 8;
    private static final long ROOM_ID = 1L;

    @Autowired
    private BookingService bookingService;

    @Test
    void parallelDeliveriesOfSameRequestCreateSingleBooking() throws Exception {
        User user = givenUser("user", Role.USER);
        CurrentUser currentUser = new CurrentUser(user.getId(), user.getUsername(), false);
        hotelConfirms(ROOM_ID, reservationOk().withFixedDelay(100));
        BookingRequest request = new BookingRequest(ROOM_ID, null, false, START, END, "req-parallel");

        List<BookingDto> results = runConcurrently(() -> bookingService.create(currentUser, request));

        assertThat(results).extracting(BookingDto::id).containsOnly(results.get(0).id());
        assertThat(bookingRepository.count()).isEqualTo(1);
        HOTEL_SERVICE.verify(1, postRequestedFor(urlEqualTo(confirmPath(ROOM_ID))));
    }

    private List<BookingDto> runConcurrently(Callable<BookingDto> action) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<BookingDto>> futures = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            futures.add(executor.submit(() -> {
                start.await();
                return action.call();
            }));
        }
        start.countDown();
        List<BookingDto> results = new ArrayList<>();
        for (Future<BookingDto> future : futures) {
            results.add(future.get());
        }
        executor.shutdown();
        return results;
    }
}
