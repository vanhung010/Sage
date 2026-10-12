package com.sagares.saga_res_api.restaurant.dto.admin;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

public record AdminOpeningHourResponse(
        Short id,
        DayOfWeek dayOfWeek,
        LocalTime openTime,
        LocalTime closeTime,
        boolean closed,
        List<Long> impactedReservationIds
) {
    public AdminOpeningHourResponse(Short id, DayOfWeek dayOfWeek, LocalTime openTime,
                                    LocalTime closeTime, boolean closed) {
        this(id, dayOfWeek, openTime, closeTime, closed, List.of());
    }
}
