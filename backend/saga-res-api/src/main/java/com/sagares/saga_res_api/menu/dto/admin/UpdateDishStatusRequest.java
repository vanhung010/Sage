package com.sagares.saga_res_api.menu.dto.admin;

import com.sagares.saga_res_api.menu.entity.DishStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Dữ liệu thay đổi trạng thái món ăn.")
public record UpdateDishStatusRequest(
        @NotNull(message = "Trạng thái món ăn là bắt buộc.")
        DishStatus status
) {
}
