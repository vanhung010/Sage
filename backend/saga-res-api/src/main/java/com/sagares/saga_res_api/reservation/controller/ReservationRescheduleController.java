package com.sagares.saga_res_api.reservation.controller;

import com.sagares.saga_res_api.reservation.dto.RescheduleReservationRequest;
import com.sagares.saga_res_api.reservation.dto.ReservationResponse;
import com.sagares.saga_res_api.reservation.service.ReservationRescheduleService;
import com.sagares.saga_res_api.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customer/reservations")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class ReservationRescheduleController {

    private final ReservationRescheduleService service;

    @PostMapping("/{id}/reschedule")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Xác nhận đổi lịch đơn đặt bàn của tôi")
    public ReservationResponse reschedule(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable("id") Long id,
            @Valid @RequestBody RescheduleReservationRequest request
    ) {
        return service.reschedule(
                user.accountId(),
                id,
                request
        );
    }
}