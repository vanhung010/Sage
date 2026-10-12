package com.sagares.saga_res_api.menu.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Administrative menu category representation.")
public record AdminCategoryResponse(
        @Schema(description = "Unique category ID.", example = "10")
        Long id,
        @Schema(description = "Unique category name.", example = "Main Course")
        String name,
        @Schema(description = "Non-negative ordering value used to sort categories.", example = "2", minimum = "0")
        int displayOrder,
        @Schema(description = "Whether the category is active and available for normal use.", example = "true")
        boolean active
) {
}
