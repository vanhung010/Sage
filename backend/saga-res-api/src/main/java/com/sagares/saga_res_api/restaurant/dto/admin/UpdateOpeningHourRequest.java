package com.sagares.saga_res_api.restaurant.dto.admin;

import com.sagares.saga_res_api.restaurant.dto.DayOfWeekDeserializer;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record UpdateOpeningHourRequest(
        // Optional for clients echoing the response; the weekday primary key cannot change.
        @JsonDeserialize(using = DayOfWeekDeserializer.class)
        DayOfWeek dayOfWeek,
        LocalTime openTime,
        LocalTime closeTime
) {
}
