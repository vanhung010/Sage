package com.sagares.saga_res_api.reservation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.OffsetDateTime;

public record CreateReservationRequest(

        // Frontend gửi cả múi giờ, ví dụ: 2026-10-18T19:00:00+07:00.
        @NotNull(message = "Vui lòng chọn thời gian bắt đầu")
        OffsetDateTime startTime,

        @NotNull(message = "Vui lòng chọn thời gian kết thúc")
        OffsetDateTime endTime,

        @NotNull(message = "Vui lòng nhập số người")
        @Min(value = 1, message = "Số người phải lớn hơn 0")
        @Max(value = 12, message = "Trên 12 người vui lòng liên hệ nhà hàng")
        Integer partySize,

        // null nghĩa là khách không yêu cầu khu vực cụ thể.
        @Positive(message = "Mã khu vực phải lớn hơn 0")
        Long requestedAreaId,

        String specialNote

        // Không nhận customerId: backend lấy từ JWT.
        // Không nhận tableId hoặc status: backend tự quyết định.
) {
}