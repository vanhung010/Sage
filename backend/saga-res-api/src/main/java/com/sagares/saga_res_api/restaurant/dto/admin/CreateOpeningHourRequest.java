package com.sagares.saga_res_api.restaurant.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import com.sagares.saga_res_api.restaurant.dto.DayOfWeekDeserializer;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Schema(description = "Request to create the opening-hour entry for one weekday.")
public record CreateOpeningHourRequest(
        @Schema(description = "Weekday to configure. Numeric enum ordinals are rejected.", example = "MONDAY", allowableValues = {"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"})
        @NotNull(message = "Ngày trong tuần là bắt buộc")
        @JsonDeserialize(using = DayOfWeekDeserializer.class)
        DayOfWeek dayOfWeek,
        @Schema(description = "Required local opening time.", example = "10:00:00", type = "string", format = "time")
        @NotNull(message = "Giờ mở cửa là bắt buộc")
        LocalTime openTime,
        @Schema(description = "Required local closing time. Must be later than openTime.", example = "22:00:00", type = "string", format = "time")
        @NotNull(message = "Giờ đóng cửa là bắt buộc")
        LocalTime closeTime
) {
}
