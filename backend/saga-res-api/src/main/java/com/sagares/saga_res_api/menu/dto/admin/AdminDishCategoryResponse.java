package com.sagares.saga_res_api.menu.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Thông tin danh mục trong dữ liệu món ăn.")
public record AdminDishCategoryResponse(Long id, String name) {
}
