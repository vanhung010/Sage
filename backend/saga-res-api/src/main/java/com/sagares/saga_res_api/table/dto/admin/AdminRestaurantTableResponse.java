package com.sagares.saga_res_api.table.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Administrative restaurant table representation.")
public record AdminRestaurantTableResponse(
        @Schema(description = "Unique table ID.", example = "100")
        Long id,
        @Schema(description = "Unique table code.", example = "T01")
        String code,
        @Schema(description = "Area containing the table.")
        AdminTableAreaResponse area,
        @Schema(description = "Smallest party size suitable for this table.", example = "2", minimum = "1")
        Short minCapacity,
        @Schema(description = "Largest party size suitable for this table; never smaller than minCapacity.", example = "4", minimum = "1")
        Short maxCapacity,
        @Schema(description = "Whether the table is active and eligible for normal assignment.", example = "true")
        boolean active
) {
}
