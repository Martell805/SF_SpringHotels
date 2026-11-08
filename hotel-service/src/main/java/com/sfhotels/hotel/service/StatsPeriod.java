package com.sfhotels.hotel.service;

import com.sfhotels.common.error.BadRequestException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public record StatsPeriod(LocalDate from, LocalDate to) {

    private static final int DEFAULT_DAYS = 30;

    public StatsPeriod {
        if (!from.isBefore(to)) {
            throw new BadRequestException("Parameter 'from' must be before 'to'");
        }
    }

    public static StatsPeriod of(LocalDate from, LocalDate to) {
        LocalDate start = from != null ? from : LocalDate.now();
        return new StatsPeriod(start, to != null ? to : start.plusDays(DEFAULT_DAYS));
    }

    public long nights() {
        return ChronoUnit.DAYS.between(from, to);
    }

    public long overlapNights(LocalDate start, LocalDate end) {
        LocalDate overlapStart = start.isAfter(from) ? start : from;
        LocalDate overlapEnd = end.isBefore(to) ? end : to;
        return Math.max(0, ChronoUnit.DAYS.between(overlapStart, overlapEnd));
    }

    public double occupancyPercent(long bookedNights, long rooms) {
        long capacity = nights() * rooms;
        return capacity == 0 ? 0 : Math.round(bookedNights * 10000.0 / capacity) / 100.0;
    }
}
