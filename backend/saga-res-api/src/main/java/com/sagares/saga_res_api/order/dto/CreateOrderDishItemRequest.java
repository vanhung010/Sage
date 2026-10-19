package com.sagares.saga_res_api.order.dto;

public record CreateOrderDishItemRequest(
        int quantity,
        Long idDish,
        int price
) {
}
