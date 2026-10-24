package com.sagares.saga_res_api.reservation.dto;

import jakarta.validation.constraints.NotBlank;

public record RejectReservationRequest(
        @NotBlank(message = "Lý do từ chối không được để trống")
        String reason
) {
}