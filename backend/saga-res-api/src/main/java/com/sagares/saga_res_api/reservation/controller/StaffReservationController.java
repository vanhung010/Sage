package com.sagares.saga_res_api.reservation.controller;

import com.sagares.saga_res_api.common.dto.PageResponse;
import com.sagares.saga_res_api.reservation.dto.CreateGuestReservationRequest;
import com.sagares.saga_res_api.reservation.dto.CreateGuestReservationResponse;
import com.sagares.saga_res_api.reservation.dto.StaffReservationStatisticsResponse;
import com.sagares.saga_res_api.reservation.dto.StaffReservationSummaryResponse;
import com.sagares.saga_res_api.reservation.entity.ReservationStatus;
import com.sagares.saga_res_api.reservation.service.StaffReservationService;
import com.sagares.saga_res_api.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/staff/reservations")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
public class StaffReservationController {

    private final StaffReservationService staffReservationService;

    @GetMapping
    @Operation(
            summary = "Tra cứu danh sách đặt bàn cho nhân viên",
            description = """
                    Bắt buộc truyền date theo định dạng yyyy-MM-dd.
                    Ngày khách đến được hiểu theo múi giờ Việt Nam.
                    Có thể tìm theo mã đơn, tên hoặc số điện thoại.
                    Có thể lọc theo trạng thái và khu vực của bàn.
                    Kết quả luôn sắp xếp startTime ASC, id ASC.
                    """
    )
    public PageResponse<StaffReservationSummaryResponse> list(
            @RequestParam(name = "date")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,

            @RequestParam(name = "search", required = false)
            String search,

            @RequestParam(name = "status", required = false)
            ReservationStatus status,

            @RequestParam(name = "areaId", required = false)
            Long areaId,

            @RequestParam(name = "page", defaultValue = "0")
            int page,

            @RequestParam(name = "size", defaultValue = "10")
            int size
    ) {
        return staffReservationService.list(
                date,
                search,
                status,
                areaId,
                page,
                size
        );
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    @Operation(
            summary = "Đếm đơn chờ duyệt trong kết quả đang xem",
            description = """
                Áp dụng cùng bộ lọc date, search, status và areaId
                với API danh sách đặt bàn.

                date là bắt buộc, theo múi giờ Asia/Ho_Chi_Minh.

                Nếu status khác PENDING thì pendingCount bằng 0.
                Số đếm không phụ thuộc phân trang.
                """
    )
    public StaffReservationStatisticsResponse getSummary(
            @RequestParam(name = "date")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,

            @RequestParam(name = "search", required = false)
            String search,

            @RequestParam(name = "status", required = false)
            ReservationStatus status,

            @RequestParam(name = "areaId", required = false)
            Long areaId
    ) {
        return staffReservationService.getSummary(
                date,
                search,
                status,
                areaId
        );
    }

    @PostMapping("/guest")
    @Operation(summary = "Nhân viên đặt bàn hộ khách")
    public ResponseEntity<CreateGuestReservationResponse> create(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody CreateGuestReservationRequest request
    ) {
        CreateGuestReservationResponse response =
                staffReservationService.create(user.accountId(), request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}