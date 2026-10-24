package com.sagares.saga_res_api.reservation.event;

// Chỉ mang dữ liệu cần gửi mail, không mang entity LAZY.
public record ReservationRejectedEvent(
        String recipient,
        String reservationCode,
        String reason
) {
}