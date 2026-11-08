package com.sfhotels.booking.client;

import com.sfhotels.booking.client.dto.ReleaseRequest;
import com.sfhotels.booking.client.dto.ReservationRequest;
import com.sfhotels.booking.client.dto.RoomDto;
import java.time.LocalDate;
import java.util.List;

public interface HotelClient {

    List<RoomDto> recommend(Long hotelId, LocalDate startDate, LocalDate endDate);

    void confirmAvailability(Long roomId, ReservationRequest request);

    void release(Long roomId, ReleaseRequest request);
}
