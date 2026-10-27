package com.sagares.saga_res_api.order.controller;

import com.sagares.saga_res_api.common.dto.PageResponse;
import com.sagares.saga_res_api.order.dto.StaffOrderDetailResponse;
import com.sagares.saga_res_api.order.dto.StaffOrderSummaryResponse;
import com.sagares.saga_res_api.order.entity.FoodOrderStatus;
import com.sagares.saga_res_api.order.service.StaffOrderService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/staff/orders")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
public class StaffOrderController {

    private final StaffOrderService staffOrderService;

    @GetMapping
    @Operation(
            summary = "Tra cứu danh sách đơn đặt món cho nhân viên",
            description = """
                    Tìm theo mã đơn, tên khách hoặc số điện thoại.
                    date là ngày tạo đơn theo Asia/Ho_Chi_Minh.
                    Bỏ date để xem tất cả ngày.
                    status gồm PLACED và CANCELLED.
                    Kết quả sắp xếp createdAt DESC, id DESC.
                    """
    )
    public PageResponse<StaffOrderSummaryResponse> list(
            @RequestParam(name = "search", required = false)
            String search,

            @RequestParam(name = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,

            @RequestParam(name = "status", required = false)
            FoodOrderStatus status,

            @RequestParam(name = "page", defaultValue = "0")
            int page,

            @RequestParam(name = "size", defaultValue = "7")
            int size
    ) {
        return staffOrderService.list(
                search,
                date,
                status,
                page,
                size
        );
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Lấy chi tiết đơn đặt món cho nhân viên",
            description = """
                    Dành cho STAFF và ADMIN.
                    Trả thông tin khách hàng, món đã đặt,
                    thông tin nhận hàng, trạng thái và tổng tiền.

                    Hỗ trợ cả PLACED và CANCELLED.
                    Giá món lấy từ snapshot unitPrice của đơn.
                    Thời gian trả dưới dạng Instant.
                    """
    )
    public StaffOrderDetailResponse getDetail(
            @PathVariable("id") Long id
    ) {
        return staffOrderService.getDetail(id);
    }
}