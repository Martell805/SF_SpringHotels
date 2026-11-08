package com.sfhotels.booking.entity;

public enum BookingStatus {
    PENDING,
    CONFIRMED,
    CANCELLED;

    public boolean canTransitionTo(BookingStatus target) {
        return switch (this) {
            case PENDING -> target != PENDING;
            case CONFIRMED -> target == CANCELLED;
            case CANCELLED -> false;
        };
    }
}
