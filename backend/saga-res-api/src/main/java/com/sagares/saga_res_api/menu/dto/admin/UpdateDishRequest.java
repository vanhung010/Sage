package com.sagares.saga_res_api.menu.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "Dữ liệu cập nhật từng phần cho món ăn.")
public record UpdateDishRequest(
        Long categoryId,

        @Size(max = 255, message = "Tên món ăn không được vượt quá 255 ký tự.")
        String name,

        String description,

        @Positive(message = "Giá món ăn phải lớn hơn 0.")
        Integer price,

        @Min(value = 0, message = "Thứ tự hiển thị không được nhỏ hơn 0.")
        Integer displayOrder,

        List<Long> tagIds
) {
}
