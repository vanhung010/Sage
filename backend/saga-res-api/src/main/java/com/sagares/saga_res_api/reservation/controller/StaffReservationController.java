package com.sagares.saga_res_api.reservation.controller;

import com.sagares.saga_res_api.common.dto.PageResponse;
import com.sagares.saga_res_api.reservation.dto.*;
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
import com.sagares.saga_res_api.reservation.dto.StaffReservationDurationDto.*;
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


    @GetMapping("/{id}")
    @Operation(summary = "Xem chi tiết đơn đặt bàn")
    public StaffReservationDetailResponse getDetail(
            @PathVariable("id") Long id
    ) {
        return staffReservationService.getDetail(id);
    }

    @PostMapping("/guest")
    @Operation(summary = "Nhân viên đặt bàn hộ khách")
    public ResponseEntity<CreateGuestReservationResponse> create(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody CreateGuestReservationRequest request
    ) {
        CreateGuestReservationResponse response = staffReservationService.create(user.accountId(), request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }



    @PostMapping("/{id}/approve")
    @Operation(
            summary = "Duyệt đơn đặt bàn",
            description = """
                Chỉ duyệt đơn đang PENDING và chưa hết hạn duyệt.
                Trả về chi tiết đơn sau khi chuyển sang CONFIRMED,
                bao gồm nhật ký duyệt.
                """
    )
    public StaffReservationDetailResponse approve(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable("id") Long id
    ) {
        return staffReservationService.approve(user, id);
    }

    @PostMapping("/{id}/reject")
    @Operation(
            summary = "Từ chối đơn đặt bàn",
            description = """
                Chỉ từ chối đơn đang PENDING.
                Bắt buộc cung cấp lý do.
                Hệ thống chuyển đơn sang REJECTED và ghi nhật ký.
                """
    )
    public StaffReservationDetailResponse reject(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable("id") Long id,
            @Valid @RequestBody RejectReservationRequest request
    ) {
        return staffReservationService.reject(user, id, request);
    }

    @PostMapping("/{id}/cancel")
    @Operation(
            summary = "Hủy đơn đặt bàn thay khách",
            description = """
                Chỉ hủy đơn đang PENDING hoặc CONFIRMED.
                Bắt buộc cung cấp lý do.
                Không áp dụng hạn hủy 2 giờ của khách hàng.
                Trả về chi tiết đơn sau khi hủy.
                """
    )
    public StaffReservationDetailResponse cancelForCustomer(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable("id") Long id,
            @Valid @RequestBody StaffCancelReservationRequest request
    ) {
        return staffReservationService.cancelForCustomer(user, id, request);
    }

    @PostMapping("/{id}/arrive")
    @Operation(
            summary = "Ghi nhận khách đã đến",
            description = """
                Chỉ áp dụng cho đơn đang CONFIRMED.
                Chỉ được thực hiện trong ngày hẹn theo múi giờ Việt Nam.
                Chuyển đơn sang ARRIVED và ghi nhật ký.
                """
    )
    public StaffReservationDetailResponse markArrived(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable("id") Long id
    ) {
        return staffReservationService.markArrived(user, id);
    }

    @PostMapping("/{id}/no-show")
    @Operation(
            summary = "Ghi nhận khách không đến",
            description = """
                Chỉ áp dụng cho đơn đang CONFIRMED.
                Chỉ thực hiện khi đã đủ thời gian chờ sau giờ bắt đầu.
                Chuyển đơn sang NO_SHOW và ghi nhật ký.
                """
    )
    public StaffReservationDetailResponse markNoShow(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable("id") Long id
    ) {
        return staffReservationService.markNoShow(user, id);
    }

    @PostMapping("/{id}/complete")
    @Operation(
            summary = "Hoàn thành đơn đặt bàn",
            description = """
                Chỉ áp dụng cho đơn đang ARRIVED.
                Chuyển đơn sang COMPLETED và ghi nhật ký.
                Giữ nguyên khoảng chiếm bàn đến hết thời gian dọn bàn.
                """
    )
    public StaffReservationDetailResponse markCompleted(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable("id") Long id
    ) {
        return staffReservationService.markCompleted(user, id);
    }

    @PostMapping("/{id}/change-duration/preview")
    @Operation(summary = "Xem trước thay đổi thời lượng đặt bàn")
    public PreviewResponse previewDuration(
            @PathVariable Long id,
            @Valid @RequestBody PreviewRequest request
    ) {
        return staffReservationService.previewDuration(id, request);
    }

    @PostMapping("/{id}/change-duration")
    @Operation(summary = "Nhân viên thay đổi thời lượng đặt bàn")
    public ChangeResponse changeDuration(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long id,
            @Valid @RequestBody ChangeRequest request
    ) {
        return staffReservationService.changeDuration(
                currentUser,
                id,
                request
        );
    }
    @GetMapping("/{id}/move-table/options")
    @Operation(
            summary = "List table options for moving a reservation",
            description = """
            Returns the reservation context, area filters and table options.
            Optional areaId filters the displayed tables.

            Only PENDING, CONFIRMED and ARRIVED reservations are supported.
            Availability includes the reservation's stored cleanup buffer.
            Unavailable tables include a reason and cannot be selected.

            This endpoint does not hold a table.
            Availability must be checked again when confirming the move.
            """
    )
    public StaffMoveTableOptionsResponse getMoveTableOptions(
            @PathVariable("id") Long id,
            @RequestParam(name = "areaId", required = false) Long areaId
    ) {
        return staffReservationService.getMoveTableOptions(id, areaId);
    }

    @PostMapping("/{id}/move-table")
    @Operation(
            summary = "Move a reservation to another table",
            description = """
            Available to STAFF and ADMIN.
            Supports PENDING, CONFIRMED and ARRIVED reservations.

            Rechecks table activity, capacity, reservation overlap
            and suspension inside the transaction.

            Preserves reservation times, status, requested area,
            cleanup buffer and customer reschedule count.

            Returns the updated reservation detail and audit history.
            """
    )
    public StaffReservationDetailResponse moveTable(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable("id") Long id,
            @Valid @RequestBody MoveTableRequest request
    ) {
        return staffReservationService.moveTable(
                currentUser,
                id,
                request
        );
    }

    @GetMapping("/timeline")
    @Operation(
            summary = "Lấy lịch đặt bàn theo ngày",
            description = """
                    Dành cho STAFF và ADMIN.
                    Ngày được hiểu theo Asia/Ho_Chi_Minh.
                    Bỏ areaId để xem tất cả khu vực.
                    Trả dữ liệu theo bàn, gồm đơn chiếm bàn và thời gian tạm ngưng.
                    Ngày đóng cửa vẫn trả dữ liệu đã tồn tại.
                    """
    )
    public StaffReservationTimelineResponse getTimeline(
            @RequestParam(name = "date")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,

            @RequestParam(name = "areaId", required = false)
            Long areaId
    ) {
        return staffReservationService.getTimeline(date, areaId);
    }
}