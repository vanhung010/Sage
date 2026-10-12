package com.sagares.saga_res_api.restaurant.dto.admin;

import com.sagares.saga_res_api.restaurant.dto.DayOfWeekDeserializer;
import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Schema(description = "Partial opening-hour update. At least one field must be supplied; the weekday cannot be changed.")
public record UpdateOpeningHourRequest(
        // Optional for clients echoing the response; the weekday primary key cannot change.
        @Schema(description = "Optional echoed weekday. If supplied, it must match the weekday identified by the path ID. Numeric ordinals are rejected.", example = "MONDAY", allowableValues = {"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"}, nullable = true)
        @JsonDeserialize(using = DayOfWeekDeserializer.class)
        DayOfWeek dayOfWeek,
        @Schema(description = "New local opening time. The resulting opening time must be earlier than the closing time.", example = "09:30:00", type = "string", format = "time", nullable = true)
        LocalTime openTime,
        @Schema(description = "New local closing time. The resulting closing time must be later than the opening time.", example = "22:30:00", type = "string", format = "time", nullable = true)
        LocalTime closeTime
) {
}
