package com.sagares.saga_res_api.order.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateOrderDishItemRequest(
        @Positive
        int quantity,
        @NotNull
        Long idDish
) {
}
