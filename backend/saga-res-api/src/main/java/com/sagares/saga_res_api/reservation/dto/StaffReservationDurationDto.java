package com.sagares.saga_res_api.reservation.dto;

import com.sagares.saga_res_api.reservation.entity.ReservationStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;

public final class StaffReservationDurationDto {

    private StaffReservationDurationDto() {
    }

    public record PreviewRequest(
            @NotNull(message = "Giờ kết thúc là bắt buộc")
            OffsetDateTime endTime
    ) {
    }

    public record ChangeRequest(
            @NotNull(message = "Giờ kết thúc là bắt buộc")
            OffsetDateTime endTime,

            @NotNull(message = "Phiên bản đơn đặt bàn là bắt buộc")
            @PositiveOrZero(message = "Phiên bản không được âm")
            Long expectedVersion,

            @Size(
                    max = 1000,
                    message = "Lý do không được vượt quá 1000 ký tự"
            )
            String reason
    ) {
    }

    public record Conflict(
            String code,
            String message,
            OffsetDateTime blockedFrom,
            OffsetDateTime blockedUntil
    ) {
    }

    public record PreviewResponse(
            Long reservationId,
            Long version,
            ReservationStatus status,
            OffsetDateTime startTime,
            OffsetDateTime currentEndTime,
            OffsetDateTime endTime,
            int bufferMinutes,
            long currentDurationMinutes,
            long durationMinutes,
            long differenceMinutes,
            OffsetDateTime occupiedUntil,
            boolean allowed,
            List<Conflict> conflicts,
            OffsetDateTime suggestedEndTime
    ) {
    }

    public record ChangeResponse(
            Long reservationId,
            String reservationCode,
            Long version,
            ReservationStatus status,
            OffsetDateTime startTime,
            OffsetDateTime endTime,
            long durationMinutes,
            int bufferMinutes,
            OffsetDateTime occupiedUntil,
            OffsetDateTime updatedAt
    ) {
    }
}