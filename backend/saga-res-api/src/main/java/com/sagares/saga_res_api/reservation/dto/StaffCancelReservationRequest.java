package com.sagares.saga_res_api.reservation.dto;

import jakarta.validation.constraints.NotBlank;

public record StaffCancelReservationRequest(
        @NotBlank(message = "Lý do hủy không được để trống")
        String reason
) {
}