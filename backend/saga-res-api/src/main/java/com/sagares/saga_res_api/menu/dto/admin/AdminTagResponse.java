package com.sagares.saga_res_api.menu.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Administrative menu tag representation.")
public record AdminTagResponse(
        @Schema(description = "Unique tag ID.", example = "10")
        Long id,
        @Schema(description = "Unique tag name.", example = "Best Seller")
        String name
) {
}
