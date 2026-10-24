package com.sagares.saga_res_api.reservation.dto;

import java.time.OffsetDateTime;

public record CreateGuestReservationResponse(
        String reservationCode,
        String name,
        String tableCode,
        int partySize,
        OffsetDateTime startTime,
        OffsetDateTime endTime
) {
}