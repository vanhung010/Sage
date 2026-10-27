package com.sagares.saga_res_api.reservation.dto;

import com.sagares.saga_res_api.reservation.entity.ReservationStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record StaffReservationTimelineResponse(
        LocalDate date,
        String timezone,
        OpeningHours openingHours,
        int gridStepMinutes,
        List<TableItem> tables
) {

    public record OpeningHours(
            boolean closed,
            LocalTime openTime,
            LocalTime closeTime,
            String closedReason
    ) {
    }

    public record AreaItem(
            Long id,
            String name,
            boolean active
    ) {
    }

    public record TableItem(
            Long id,
            String code,
            boolean active,
            AreaItem area,
            int minCapacity,
            int maxCapacity,
            List<ReservationItem> reservations,
            List<SuspensionItem> suspensions
    ) {
    }

    public record ReservationItem(
            Long id,
            String code,
            String customerName,
            int partySize,
            ReservationStatus status,
            Instant startTime,
            Instant endTime,
            int bufferMinutes,
            Instant occupiedUntil
    ) {
    }

    public record SuspensionItem(
            Long id,
            Instant startTime,
            Instant endTime,
            String reason
    ) {
    }
}