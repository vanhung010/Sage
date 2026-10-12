package com.sagares.saga_res_api.restaurant.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

@Schema(description = "Administrative weekly opening-hour entry, including reservations impacted by an update.")
public record AdminOpeningHourResponse(
        @Schema(description = "Weekday key used by the update endpoint: 0 for Sunday and 1 through 6 for Monday through Saturday.", example = "1", minimum = "0", maximum = "6")
        Short id,
        @Schema(description = "Weekday represented by this schedule entry.", example = "MONDAY", allowableValues = {"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"})
        DayOfWeek dayOfWeek,
        @Schema(description = "Local opening time; may be null for a closed weekday.", example = "10:00:00", type = "string", format = "time", nullable = true)
        LocalTime openTime,
        @Schema(description = "Local closing time, later than openTime; may be null for a closed weekday.", example = "22:00:00", type = "string", format = "time", nullable = true)
        LocalTime closeTime,
        @Schema(description = "Whether the restaurant is closed for this weekday.", example = "false")
        boolean closed,
        @Schema(description = "IDs of active future reservations now outside the updated hours. Empty for list and create responses.", example = "[101, 102]")
        List<Long> impactedReservationIds
) {
    public AdminOpeningHourResponse(Short id, DayOfWeek dayOfWeek, LocalTime openTime,
                                    LocalTime closeTime, boolean closed) {
        this(id, dayOfWeek, openTime, closeTime, closed, List.of());
    }
}
