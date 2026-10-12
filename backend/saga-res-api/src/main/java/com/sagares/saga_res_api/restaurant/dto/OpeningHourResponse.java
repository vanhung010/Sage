package com.sagares.saga_res_api.restaurant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.DayOfWeek;
import java.time.LocalTime;

@Schema(description = "Public weekly opening-hour entry.")
public record OpeningHourResponse(
        @Schema(description = "Weekday represented by this schedule entry.", example = "MONDAY", allowableValues = {"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"})
        DayOfWeek dayOfWeek,
        @Schema(description = "Local opening time; may be null for a closed weekday.", example = "10:00:00", type = "string", format = "time", nullable = true)
        LocalTime openTime,
        @Schema(description = "Local closing time, later than openTime; may be null for a closed weekday.", example = "22:00:00", type = "string", format = "time", nullable = true)
        LocalTime closeTime,
        @Schema(description = "Whether the restaurant is closed for this weekday.", example = "false")
        boolean closed
) {
}
