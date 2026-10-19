package com.sagares.saga_res_api.order.dto;

import com.sagares.saga_res_api.order.entity.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateOrderDishRequest(
        @NotEmpty
        List<@NotNull @Valid CreateOrderDishItemRequest> createOrderDishItemRequestList,
        @NotBlank
        String deliveryAddress,
        @NotBlank
        String deliveryPhone,
        PaymentMethod paymentMethod
) {
}
