package com.sagares.saga_res_api.menu.dto.admin;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateCategoryRequest(
        @Size(max = 100)
        String name,

        @Min(0)
        Integer displayOrder,

        Boolean active
) {
}
