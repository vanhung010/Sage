package com.sagares.saga_res_api.table.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Compact area information embedded in a restaurant table response.")
public record AdminTableAreaResponse(
        @Schema(description = "Unique area ID.", example = "10")
        Long id,
        @Schema(description = "Area name.", example = "Garden")
        String name
) {
}
