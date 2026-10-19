package com.sagares.saga_res_api.order.dto;

import com.sagares.saga_res_api.order.entity.PaymentMethod;

import java.util.List;

public record CreateOrderDishRequest(
        List<CreateOrderDishItemRequest> createOrderDishItemRequestList,
        String deliveryAddress,
        String deliveryPhone,
        PaymentMethod paymentMethod
) {
}
