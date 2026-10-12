package com.sagares.saga_res_api.menu.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTagRequest(
        @NotBlank(message = "Tên nhãn không được để trống")
        @Size(max = 50, message = "Tên nhãn không được vượt quá 50 ký tự")
        String name
) {
}
