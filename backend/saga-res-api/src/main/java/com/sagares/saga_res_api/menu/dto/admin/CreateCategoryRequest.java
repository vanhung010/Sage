package com.sagares.saga_res_api.menu.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to create a menu category.")
public record CreateCategoryRequest(
        @Schema(description = "Unique category name. Must be non-blank and at most 100 characters.", example = "Main Course", maxLength = 100)
        @NotBlank
        @Size(max = 100)
        String name,

        @Schema(description = "Non-negative ordering value used when listing categories.", example = "2", minimum = "0")
        @NotNull
        @Min(0)
        Integer displayOrder
) {
}
