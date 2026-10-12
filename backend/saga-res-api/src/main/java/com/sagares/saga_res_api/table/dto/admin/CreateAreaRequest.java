package com.sagares.saga_res_api.table.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to create a restaurant seating area.")
public record CreateAreaRequest(
        @Schema(description = "Unique area name. Must be non-blank and at most 100 characters.", example = "Garden", maxLength = 100)
        @NotBlank
        @Size(max = 100)
        String name,

        @Schema(description = "Optional free-form area description.", example = "Outdoor seating with garden views", nullable = true)
        String description
) {
}
