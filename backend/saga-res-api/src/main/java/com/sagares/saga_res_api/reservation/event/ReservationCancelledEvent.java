package com.sagares.saga_res_api.reservation.event;

public record ReservationCancelledEvent(
        String recipient,
        String reservationCode,
        String reason
) {
}