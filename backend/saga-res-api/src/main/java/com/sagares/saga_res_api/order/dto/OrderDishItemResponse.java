package com.sagares.saga_res_api.order.dto;

public record OrderDishItemResponse(
        String nameDish,
        int quantity,
        int unitPrice,
        int lineTotal
) {
}
