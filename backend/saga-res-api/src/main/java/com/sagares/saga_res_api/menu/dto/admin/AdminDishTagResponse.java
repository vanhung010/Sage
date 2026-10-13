package com.sagares.saga_res_api.menu.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Thông tin nhãn trong dữ liệu món ăn.")
public record AdminDishTagResponse(Long id, String name) {
}
