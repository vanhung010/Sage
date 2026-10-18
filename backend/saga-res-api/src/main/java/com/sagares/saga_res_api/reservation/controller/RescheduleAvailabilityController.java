package com.sagares.saga_res_api.reservation.controller;
import com.sagares.saga_res_api.reservation.dto.AvailabilityResponse;
import com.sagares.saga_res_api.reservation.service.RescheduleAvailabilityService;
import com.sagares.saga_res_api.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/customer/reservations/{id}/availability")
@RequiredArgsConstructor
@Tag(name = "Reservation Rescheduling")
@SecurityRequirement(name = "bearerAuth")
public class RescheduleAvailabilityController {

    private final RescheduleAvailabilityService rescheduleAvailabilityService;

    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(
            summary = "Tìm giờ trống để đổi lịch đơn đặt bàn của tôi",
            description = """
                    Kiểm tra quyền sở hữu, trạng thái, hạn đổi và số lần đổi còn lại.
                    Loại trừ chính đơn đang sửa khi tính giờ trống và lịch trùng của khách.
                    Thời gian theo Asia/Ho_Chi_Minh.
                    Chỉ tra cứu, không giữ chỗ hoặc thay đổi đơn.
                    """
    )
    public AvailabilityResponse search(
            @AuthenticationPrincipal AuthenticatedUser currentUser,

            @PathVariable("id")
            Long reservationId,

            @RequestParam("date")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,

            @RequestParam("partySize")
            int partySize,

            @RequestParam("durationMinutes")
            int durationMinutes,

            @RequestParam(value = "areaId", required = false)
            Long areaId
    ) {
        return rescheduleAvailabilityService.search(
                currentUser,
                reservationId,
                date,
                partySize,
                durationMinutes,
                areaId
        );
    }
}
