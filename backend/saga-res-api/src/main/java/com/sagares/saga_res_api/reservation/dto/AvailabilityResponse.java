package com.sagares.saga_res_api.reservation.dto;


import java.time.LocalDate;
import java.util.List;

public record AvailabilityResponse(
        LocalDate date,
        int partySize,
        int durationMinutes,
        Long areaId,
        List<AvailabilitySlotResponse> slots,
        List<AvailabilitySlotResponse> suggestions
) {
}
