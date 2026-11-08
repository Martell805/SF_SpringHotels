package com.sfhotels.hotel.config;

import com.sfhotels.hotel.entity.Hotel;
import com.sfhotels.hotel.entity.ReservationStatus;
import com.sfhotels.hotel.entity.Room;
import com.sfhotels.hotel.entity.RoomReservation;
import com.sfhotels.hotel.repository.HotelRepository;
import com.sfhotels.hotel.repository.RoomReservationRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.demo-data.enabled", havingValue = "true")
public class DemoDataInitializer implements ApplicationRunner {

    private final HotelRepository hotelRepository;
    private final RoomReservationRepository reservationRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Hotel neva = hotel("Grand Neva", "Nevsky prospekt, 1", "Saint Petersburg");
        Room neva101 = room(neva, "101", 2, "5500", true, 3);
        room(neva, "102", 2, "5500", true, 0);
        room(neva, "103", 3, "7200", true, 1);
        room(neva, "201", 4, "12000", false, 5);

        Hotel central = hotel("Moscow Central", "Tverskaya street, 10", "Moscow");
        Room central11 = room(central, "11", 1, "4000", true, 2);
        Room central12 = room(central, "12", 2, "6000", true, 2);
        room(central, "13", 2, "6000", true, 0);

        Hotel riverside = hotel("Kazan Riverside", "Kremlevskaya street, 5", "Kazan");
        room(riverside, "1", 2, "3500", true, 0);
        room(riverside, "2", 2, "3500", true, 4);

        hotelRepository.save(neva);
        hotelRepository.save(central);
        hotelRepository.save(riverside);

        LocalDate today = LocalDate.now();
        reservation(neva101, "seed-booking-1", 1L, today.plusDays(5), today.plusDays(8), ReservationStatus.ACTIVE);
        reservation(central11, "seed-booking-2", 2L, today.plusDays(10), today.plusDays(12),
                ReservationStatus.ACTIVE);
        reservation(central12, "seed-booking-3", 3L, today.plusDays(3), today.plusDays(4),
                ReservationStatus.RELEASED);
        log.info("Demo data loaded: {} hotels, {} reservations", hotelRepository.count(),
                reservationRepository.count());
    }

    private Hotel hotel(String name, String address, String city) {
        Hotel hotel = new Hotel();
        hotel.setName(name);
        hotel.setAddress(address);
        hotel.setCity(city);
        return hotel;
    }

    private Room room(Hotel hotel, String number, int capacity, String price, boolean available, long timesBooked) {
        Room room = new Room();
        room.setHotel(hotel);
        room.setNumber(number);
        room.setCapacity(capacity);
        room.setPricePerNight(new BigDecimal(price));
        room.setAvailable(available);
        room.setTimesBooked(timesBooked);
        hotel.getRooms().add(room);
        return room;
    }

    private void reservation(Room room, String requestId, Long bookingId, LocalDate start, LocalDate end,
                             ReservationStatus status) {
        RoomReservation reservation = new RoomReservation();
        reservation.setRoom(room);
        reservation.setRequestId(requestId);
        reservation.setBookingId(bookingId);
        reservation.setStartDate(start);
        reservation.setEndDate(end);
        reservation.setStatus(status);
        reservationRepository.save(reservation);
    }
}
