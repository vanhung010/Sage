package com.sagares.saga_res_api.reservation.dto;

import com.sagares.saga_res_api.account.entity.AccountRole;
import com.sagares.saga_res_api.reservation.entity.ReservationLogEventType;
import com.sagares.saga_res_api.reservation.entity.ReservationStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record StaffReservationDetailResponse(
        Long id,
        String code,
        ReservationStatus status,
        BookingSource source,

        String customerName,
        String customerPhone,

        int partySize,
        OffsetDateTime startTime,
        OffsetDateTime endTime,

        TableSummary table,
        AreaSummary requestedArea,

        String specialNote,
        int bufferMinutes,
        int rescheduleCount,

        String cancellationReason,
        String rejectionReason,

        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,

        OffsetDateTime approvalDeadline,
        OffsetDateTime noShowAllowedAt,

        List<LogResponse> logs
) {

    public enum BookingSource {
        ONLINE,
        STAFF_GUEST
    }

    public record AreaSummary(
            Long id,
            String name
    ) {
    }

    public record TableSummary(
            Long id,
            String code,
            AreaSummary area
    ) {
    }

    public record LogResponse(
            Long id,
            ReservationLogEventType eventType,
            OffsetDateTime changedAt,

            String actorName,
            AccountRole actorRole,

            Map<String, Object> oldValue,
            Map<String, Object> newValue,
            String reason
    ) {
    }
}