package com.sagares.saga_res_api.restaurant.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record OpeningHourResponse(
        DayOfWeek dayOfWeek,
        LocalTime openTime,
        LocalTime closeTime,
        boolean closed
) {
}
