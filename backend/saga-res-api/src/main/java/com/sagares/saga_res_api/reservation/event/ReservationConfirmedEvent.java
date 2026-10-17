package com.sagares.saga_res_api.reservation.event;

import com.sagares.saga_res_api.reservation.dto.ReservationResponse;

// Sự kiện thông báo rằng một đơn đặt bàn đã được xác nhận.
// Record này chỉ chứa dữ liệu, không trực tiếp gửi email.
public record ReservationConfirmedEvent(

        // Địa chỉ email của khách nhận thông báo.
        String recipient,

        // Thông tin đơn đặt bàn dùng để tạo nội dung email.
        // Dùng DTO để tránh truy cập entity LAZY ngoài transaction.
        ReservationResponse reservation

) {
}