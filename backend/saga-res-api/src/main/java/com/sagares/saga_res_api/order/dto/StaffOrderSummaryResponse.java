package com.sagares.saga_res_api.order.dto;

import com.sagares.saga_res_api.order.entity.FoodOrderStatus;
import com.sagares.saga_res_api.order.entity.PaymentMethod;

import java.time.Instant;

public record StaffOrderSummaryResponse(
        Long id,
        Long customerId,
        String customerName,
        String customerPhone,
        String deliveryPhone,
        Instant createdAt,
        FoodOrderStatus status,
        long totalQuantity,
        long totalAmount,
        PaymentMethod paymentMethod,
        Instant cancelledAt
) {
}