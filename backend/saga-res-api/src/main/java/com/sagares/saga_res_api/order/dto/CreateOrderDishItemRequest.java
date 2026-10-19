package com.sagares.saga_res_api.order.dto;

import jakarta.validation.constraints.NotNull;

public record CreateOrderDishItemRequest(
        int quantity,
        @NotNull
        Long idDish
) {
}
