package com.sagares.saga_res_api.order.dto;

import com.sagares.saga_res_api.order.entity.FoodOrder;
import com.sagares.saga_res_api.order.entity.FoodOrderItem;
import com.sagares.saga_res_api.order.entity.FoodOrderStatus;
import com.sagares.saga_res_api.order.entity.PaymentMethod;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

public record StaffOrderDetailResponse(
        Long id,

        Long customerId,
        String customerName,
        String customerPhone,

        Instant createdAt,
        Instant cancelledAt,
        FoodOrderStatus status,

        String deliveryPhone,
        String deliveryAddress,
        PaymentMethod paymentMethod,

        long dishCount,
        long totalQuantity,
        long totalAmount,

        List<Item> items
) {

    public static StaffOrderDetailResponse toDTO(
            FoodOrder foodOrder
    ) {
        // Giữ thứ tự các dòng món ổn định giữa các lần tải.
        List<Item> items = foodOrder.getItems()
                .stream()
                .sorted(Comparator.comparing(FoodOrderItem::getId))
                .map(Item::toDTO)
                .toList();

        // Số món khác nhau, phân biệt với tổng số phần.
        long dishCount = items.stream()
                .map(Item::dishId)
                .distinct()
                .count();

        long totalQuantity = items.stream()
                .mapToLong(Item::quantity)
                .sum();

        long totalAmount = items.stream()
                .mapToLong(Item::lineTotal)
                .sum();

        var customer = foodOrder.getCustomer();

        return new StaffOrderDetailResponse(
                foodOrder.getId(),

                customer.getId(),
                customer.getFullName(),
                customer.getPhone(),

                foodOrder.getCreatedAt(),
                foodOrder.getCancelledAt(),
                foodOrder.getStatus(),

                foodOrder.getDeliveryPhone(),
                foodOrder.getDeliveryAddress(),
                foodOrder.getPaymentMethod(),

                dishCount,
                totalQuantity,
                totalAmount,

                items
        );
    }

    public record Item(
            Long id,
            Long dishId,
            String dishName,
            int quantity,
            int unitPrice,
            long lineTotal
    ) {

        private static Item toDTO(FoodOrderItem item) {
            // Dùng giá đã lưu khi đặt món.
            int unitPrice = item.getUnitPrice();
            int quantity = item.getQuantity();

            // Ép kiểu trước phép nhân để phép tính dùng long.
            long lineTotal = (long) unitPrice * quantity;

            return new Item(
                    item.getId(),
                    item.getDish().getId(),
                    item.getDish().getName(),
                    quantity,
                    unitPrice,
                    lineTotal
            );
        }
    }
}