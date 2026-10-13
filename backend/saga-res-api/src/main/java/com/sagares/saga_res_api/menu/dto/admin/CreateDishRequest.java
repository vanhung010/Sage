package com.sagares.saga_res_api.menu.dto.admin;

import com.sagares.saga_res_api.menu.entity.DishStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "Dữ liệu tạo món ăn mới.")
public record CreateDishRequest(
        @NotNull(message = "Danh mục món ăn là bắt buộc.")
        Long categoryId,

        @NotBlank(message = "Tên món ăn không được để trống.")
        @Size(max = 255, message = "Tên món ăn không được vượt quá 255 ký tự.")
        String name,

        String description,

        @NotNull(message = "Giá món ăn là bắt buộc.")
        @Positive(message = "Giá món ăn phải lớn hơn 0.")
        Integer price,

        DishStatus status,

        @Min(value = 0, message = "Thứ tự hiển thị không được nhỏ hơn 0.")
        Integer displayOrder,

        List<Long> tagIds
) {
}
