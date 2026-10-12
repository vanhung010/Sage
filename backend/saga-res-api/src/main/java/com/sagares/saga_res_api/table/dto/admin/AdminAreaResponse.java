package com.sagares.saga_res_api.table.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Administrative restaurant area representation.")
public record AdminAreaResponse(
        @Schema(description = "Unique area ID.", example = "10")
        Long id,
        @Schema(description = "Unique area name.", example = "Garden")
        String name,
        @Schema(description = "Optional area description.", example = "Outdoor seating with garden views", nullable = true)
        String description,
        @Schema(description = "Whether tables may be created or moved into this area.", example = "true")
        boolean active
) {
}
