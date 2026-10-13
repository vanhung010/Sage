package com.sagares.saga_res_api.menu.dto.admin;

import com.sagares.saga_res_api.menu.entity.DishStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "Thông tin món ăn dành cho quản trị viên.")
public record AdminDishResponse(
        Long id,
        String name,
        String description,
        Integer price,
        String imageUrl,
        DishStatus status,
        int displayOrder,
        AdminDishCategoryResponse category,
        List<AdminDishTagResponse> tags,
        Instant createdAt,
        Instant updatedAt
) {
}
