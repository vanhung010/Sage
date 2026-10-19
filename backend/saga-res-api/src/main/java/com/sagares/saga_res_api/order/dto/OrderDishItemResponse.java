package com.sagares.saga_res_api.order.dto;

import com.sagares.saga_res_api.order.entity.FoodOrderItem;

public record OrderDishItemResponse(
        String nameDish,
        int quantity,
        int unitPrice,
        int lineTotal
) {
    public static OrderDishItemResponse toDTO(FoodOrderItem foodOrderItem){
        return new OrderDishItemResponse(
                foodOrderItem.getDish().getName(),
                foodOrderItem.getQuantity(),
                foodOrderItem.getUnitPrice(),
                foodOrderItem.getQuantity() * foodOrderItem.getUnitPrice()
        );
    }
}
