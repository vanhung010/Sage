package com.sagares.saga_res_api.reservation.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record MoveTableRequest(
        @NotNull(message = "Bàn cần chuyển đến là bắt buộc")
        @Positive(message = "ID bàn phải lớn hơn 0")
        Long tableId,

        @NotNull(message = "Phiên bản đơn đặt bàn là bắt buộc")
        @PositiveOrZero(message = "Phiên bản không được âm")
        Long expectedVersion,

        @Size(
                max = 1000,
                message = "Ghi chú không được vượt quá 1000 ký tự"
        )
        String reason
) {
}