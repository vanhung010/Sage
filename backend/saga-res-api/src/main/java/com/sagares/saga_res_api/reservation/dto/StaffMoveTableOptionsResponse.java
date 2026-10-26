package com.sagares.saga_res_api.reservation.dto;

import com.sagares.saga_res_api.reservation.entity.ReservationStatus;

import java.time.OffsetDateTime;
import java.util.List;

import com.sagares.saga_res_api.reservation.dto
        .StaffReservationDetailResponse.AreaSummary;
import com.sagares.saga_res_api.reservation.dto
        .StaffReservationDetailResponse.TableSummary;

public record StaffMoveTableOptionsResponse(
        ReservationContext reservation,
        List<AreaSummary> areas,
        Long selectedAreaId,
        int suitableTableCount,
        int totalTableCount,
        List<TableOption> tables
) {

    public record ReservationContext(
            Long id,
            String code,
            Long version,
            ReservationStatus status,
            String customerName,
            int partySize,
            OffsetDateTime startTime,
            OffsetDateTime endTime,
            int bufferMinutes,
            OffsetDateTime occupiedUntil,
            TableSummary currentTable,
            AreaSummary requestedArea
    ) {
    }

    public record TableOption(
            Long id,
            String code,
            AreaSummary area,
            int minCapacity,
            int maxCapacity,
            Availability availability,
            boolean selectable,
            boolean outsideRequestedArea,
            BlockingPeriod blockingPeriod,
            String unavailableReason
    ) {
    }

    /**
     * Khoảng thời gian chặn bàn, có đầu đóng và cuối mở: [startTime, endTime).
     * Với reservation, endTime đã bao gồm buffer dọn bàn.
     */
    public record BlockingPeriod(
            OffsetDateTime startTime,
            OffsetDateTime endTime
    ) {
    }

    /**
     * Kết quả kiểm tra theo khung giờ của đơn.
     * Không phải trạng thái được lưu thêm vào database.
     */
    public enum Availability {
        AVAILABLE,
        BUSY,
        CAPACITY_MISMATCH,
        SUSPENDED,
        INACTIVE
    }
}