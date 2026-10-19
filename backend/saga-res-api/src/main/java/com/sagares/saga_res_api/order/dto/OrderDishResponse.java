package com.sagares.saga_res_api.order.dto;

import com.sagares.saga_res_api.order.entity.PaymentMethod;

import java.util.List;

public record OrderDishResponse(
        String deleveryAddress,
        String deliveryPhone,
        PaymentMethod paymentMethod,
        List<OrderDishItemResponse> orderDishItemResponseList
) {
}
