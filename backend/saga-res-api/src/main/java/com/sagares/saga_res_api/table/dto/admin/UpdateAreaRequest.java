package com.sagares.saga_res_api.table.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Partial area update. At least one field must be supplied.")
public record UpdateAreaRequest(
        @Schema(description = "New unique area name. When supplied, it must be non-blank after trimming and at most 100 characters.", example = "Riverside Garden", maxLength = 100, nullable = true)
        @Size(max = 100)
        String name,

        @Schema(description = "New free-form area description.", example = "Quiet outdoor seating", nullable = true)
        String description
) {
}
