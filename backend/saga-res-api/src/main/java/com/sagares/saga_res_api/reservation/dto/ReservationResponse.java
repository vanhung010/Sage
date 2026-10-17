package com.sagares.saga_res_api.reservation.dto;

import com.sagares.saga_res_api.reservation.entity.ReservationStatus;

import java.time.OffsetDateTime;

public record ReservationResponse(
        Long id,
        String code,
        int partySize,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        ReservationStatus status,
        AreaSummary requestedArea,
        String specialNote,
        int rescheduleCount,
        String rejectionReason,
        String cancellationReason,
        OffsetDateTime createdAt
) {

    public record AreaSummary(
            Long id,
            String name,
            String description
    ) {
    }
}