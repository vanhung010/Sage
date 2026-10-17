package com.sagares.saga_res_api.reservation.dto;

import java.time.OffsetDateTime;

public record AvailabilitySlotResponse(
        OffsetDateTime startTime,
        OffsetDateTime endTime
) {
}