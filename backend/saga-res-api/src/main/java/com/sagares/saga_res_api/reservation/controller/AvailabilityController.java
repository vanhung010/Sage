package com.sagares.saga_res_api.reservation.controller;

import com.sagares.saga_res_api.reservation.dto.AvailabilityResponse;
import com.sagares.saga_res_api.reservation.service.AvailabilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/availability")
@RequiredArgsConstructor
@Tag(name = "Availability")
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    @GetMapping
    @Operation(
            summary = "Tra cứu giờ đặt bàn còn trống",
            description = """
                    Trả về các giờ bắt đầu còn ít nhất một bàn phù hợp.
                    Thời gian được diễn giải theo Asia/Ho_Chi_Minh.
                    API chỉ tra cứu, không giữ chỗ.
                    """
    )
    public AvailabilityResponse search(
            @RequestParam(name = "date")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,

            @RequestParam(name = "partySize")
            int partySize,

            @RequestParam(name = "durationMinutes")
            int durationMinutes,

            @RequestParam(name = "areaId", required = false)
            Long areaId
    ) {
        return availabilityService.search(
                date,
                partySize,
                durationMinutes,
                areaId
        );
    }
}