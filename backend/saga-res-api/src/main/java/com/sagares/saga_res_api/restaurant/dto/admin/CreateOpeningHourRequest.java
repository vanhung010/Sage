package com.sagares.saga_res_api.restaurant.dto.admin;

import jakarta.validation.constraints.NotNull;
import com.sagares.saga_res_api.restaurant.dto.DayOfWeekDeserializer;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record CreateOpeningHourRequest(
        @NotNull(message = "Ngày trong tuần là bắt buộc")
        @JsonDeserialize(using = DayOfWeekDeserializer.class)
        DayOfWeek dayOfWeek,
        @NotNull(message = "Giờ mở cửa là bắt buộc")
        LocalTime openTime,
        @NotNull(message = "Giờ đóng cửa là bắt buộc")
        LocalTime closeTime
) {
}
