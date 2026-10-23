package com.sagares.saga_res_api.reservation.dto;

import com.sagares.saga_res_api.reservation.entity.ReservationStatus;

import java.time.Instant;

public record StaffReservationSummaryResponse(
        Long id,
        String code,
        String customerName,
        String customerPhone,
        Instant startTime,
        Instant endTime,
        Integer partySize,
        Long tableId,
        String tableCode,
        Long areaId,
        String areaName,
        ReservationStatus status
) {
}