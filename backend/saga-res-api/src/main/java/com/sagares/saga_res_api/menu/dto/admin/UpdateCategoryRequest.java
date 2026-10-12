package com.sagares.saga_res_api.menu.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

@Schema(description = "Partial menu category update. At least one field must be supplied.")
public record UpdateCategoryRequest(
        @Schema(description = "New category name. When supplied, it must be non-blank after trimming, unique, and at most 100 characters.", example = "Main Courses", maxLength = 100, nullable = true)
        @Size(max = 100)
        String name,

        @Schema(description = "New non-negative display order.", example = "3", minimum = "0", nullable = true)
        @Min(0)
        Integer displayOrder,

        @Schema(description = "New active state.", example = "true", nullable = true)
        Boolean active
) {
}
