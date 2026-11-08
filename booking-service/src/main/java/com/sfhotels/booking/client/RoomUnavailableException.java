package com.sfhotels.booking.client;

import com.sfhotels.common.error.ConflictException;

public class RoomUnavailableException extends ConflictException {

    public RoomUnavailableException(String message) {
        super(message);
    }
}
