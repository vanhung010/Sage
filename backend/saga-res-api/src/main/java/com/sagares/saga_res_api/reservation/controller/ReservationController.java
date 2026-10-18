package com.sagares.saga_res_api.reservation.controller;

import com.sagares.saga_res_api.reservation.dto.CreateReservationRequest;
import com.sagares.saga_res_api.reservation.dto.ReservationResponse;
import com.sagares.saga_res_api.reservation.service.ReservationService;
import com.sagares.saga_res_api.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Khách hàng tạo đơn đặt bàn")
    public ResponseEntity<ReservationResponse> create(@AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody CreateReservationRequest request
    ) {
        // accountId lấy từ principal đã được JWT filter xác thực.
        ReservationResponse response = reservationService.create(
                user.accountId(),
                request
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Hủy đơn đặt bàn")
    public ResponseEntity<ReservationResponse> cancel(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id){
        ReservationResponse response = reservationService.cancel(user, id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ReservationResponse getDetail(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable("id") Long id
    ) {
        return reservationService.getDetail(currentUser, id);
    }
}
