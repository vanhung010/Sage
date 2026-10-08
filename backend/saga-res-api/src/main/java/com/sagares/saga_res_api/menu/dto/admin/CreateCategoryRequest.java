package com.sagares.saga_res_api.menu.dto.admin;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateCategoryRequest(
        @NotBlank
        @Size(max = 100)
        String name,

        @NotNull
        @Min(0)
        Integer displayOrder
) {
}
