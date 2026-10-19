package com.sagares.saga_res_api.order.dto;

import com.sagares.saga_res_api.order.entity.PaymentMethod;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateOrderDishRequest(
        @NotEmpty
        List<CreateOrderDishItemRequest> createOrderDishItemRequestList,
        String deliveryAddress,
        String deliveryPhone,
        PaymentMethod paymentMethod
) {
}
