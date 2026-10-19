package com.sagares.saga_res_api.order.dto;

import com.sagares.saga_res_api.order.entity.FoodOrderStatus;
import com.sagares.saga_res_api.order.entity.PaymentMethod;

import java.time.Instant;
import java.util.List;

public record OrderDishResponse(
        Long id,
        FoodOrderStatus foodOrderStatus,
        Instant createdAt,
        int totalAmount,

        String deleveryAddress,
        String deliveryPhone,
        PaymentMethod paymentMethod,
        List<OrderDishItemResponse> orderDishItemResponseList
) {
}
