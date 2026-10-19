package com.sagares.saga_res_api.order.dto;

import com.sagares.saga_res_api.order.entity.FoodOrder;
import com.sagares.saga_res_api.order.entity.FoodOrderStatus;
import com.sagares.saga_res_api.order.entity.PaymentMethod;

import java.time.Instant;
import java.util.List;

public record OrderDetailDishResponse(
        Long id,
        FoodOrderStatus foodOrderStatus,
        Instant createdAt,
        int totalAmountPrice,
        int totalQuantity,
        String deleveryAddress,
        String deliveryPhone,
        PaymentMethod paymentMethod,
        List<OrderDishItemResponse> orderDishItemResponseList
) {
    
    public static OrderDetailDishResponse toDTO(FoodOrder foodOrder){
        int totalAmountPrice = 0;
        int totalQuantity = 0;
        for (var item : foodOrder.getItems()) {
            totalAmountPrice += item.getUnitPrice() * item.getQuantity();
            totalQuantity += item.getQuantity();
        }
        List<OrderDishItemResponse> items = foodOrder.getItems().stream()
                .map(OrderDishItemResponse::toDTO)
                .toList();
        return new OrderDetailDishResponse(
                foodOrder.getId(),
                foodOrder.getStatus(),
                foodOrder.getCreatedAt(),
                totalAmountPrice,
                totalQuantity,
                foodOrder.getDeliveryAddress(),
                foodOrder.getDeliveryPhone(),
                foodOrder.getPaymentMethod(),
                items
        );
    }
}
