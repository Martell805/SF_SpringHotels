package com.sfhotels.hotel.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sfhotels.common.error.ConflictException;
import com.sfhotels.hotel.dto.ReservationRequest;
import com.sfhotels.hotel.entity.Room;
import com.sfhotels.hotel.support.IntegrationTest;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ReservationConcurrencyTest extends IntegrationTest {

    private static final int THREADS = 10;
    private static final LocalDate START = LocalDate.now().plusDays(20);

    @Autowired
    private ReservationService reservationService;

    @Test
    void concurrentOverlappingRequestsProduceSingleReservation() throws Exception {
        Room room = givenRoom(givenHotel("Neva", "Saint Petersburg"), "101", 0, true);

        List<Outcome> outcomes = runConcurrently(index -> new ReservationRequest("req-" + index, (long) index,
                START.plusDays(index % 2), START.plusDays(3)), room.getId());

        assertThat(outcomes).filteredOn(Outcome.SUCCESS::equals).hasSize(1);
        assertThat(outcomes).filteredOn(Outcome.CONFLICT::equals).hasSize(THREADS - 1);
        assertThat(reservationRepository.count()).isEqualTo(1);
        assertThat(roomRepository.findById(room.getId()).orElseThrow().getTimesBooked()).isEqualTo(1);
    }

    @Test
    void concurrentDuplicatesOfSameRequestAreAllAcceptedOnce() throws Exception {
        Room room = givenRoom(givenHotel("Neva", "Saint Petersburg"), "101", 0, true);

        List<Outcome> outcomes = runConcurrently(index -> new ReservationRequest("same-request", 1L,
                START, START.plusDays(2)), room.getId());

        assertThat(outcomes).containsOnly(Outcome.SUCCESS);
        assertThat(reservationRepository.count()).isEqualTo(1);
        assertThat(roomRepository.findById(room.getId()).orElseThrow().getTimesBooked()).isEqualTo(1);
    }

    private List<Outcome> runConcurrently(RequestFactory factory, Long roomId) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Outcome>> futures = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            ReservationRequest request = factory.create(i);
            Callable<Outcome> task = () -> {
                start.await();
                return attempt(roomId, request);
            };
            futures.add(executor.submit(task));
        }
        start.countDown();
        List<Outcome> outcomes = new ArrayList<>();
        for (Future<Outcome> future : futures) {
            outcomes.add(future.get());
        }
        executor.shutdown();
        return outcomes;
    }

    private Outcome attempt(Long roomId, ReservationRequest request) {
        try {
            reservationService.confirm(roomId, request);
            return Outcome.SUCCESS;
        } catch (ConflictException ex) {
            return Outcome.CONFLICT;
        }
    }

    private enum Outcome {
        SUCCESS,
        CONFLICT
    }

    @FunctionalInterface
    private interface RequestFactory {
        ReservationRequest create(int index);
    }
}
