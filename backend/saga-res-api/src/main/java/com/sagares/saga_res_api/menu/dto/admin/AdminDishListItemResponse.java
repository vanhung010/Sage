package com.sagares.saga_res_api.menu.dto.admin;

import com.sagares.saga_res_api.menu.entity.DishStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Một món ăn trong danh sách quản trị.")
public record AdminDishListItemResponse(
        Long id,
        String name,
        Integer price,
        String imageUrl,
        DishStatus status,
        AdminDishCategoryResponse category,

        @Schema(
                description = """
                        Nhãn đầu tiên theo thứ tự name tăng dần, sau đó id.
                        Null nếu món ăn chưa có nhãn.
                        """
        )
        AdminDishTagResponse tag
) {
}