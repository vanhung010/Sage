package com.sagares.saga_res_api.reservation.event;

import com.sagares.saga_res_api.reservation.dto.ReservationResponse;

public record ReservationRescheduledEvent(
        String recipient,
        ReservationResponse reservation
) {
}