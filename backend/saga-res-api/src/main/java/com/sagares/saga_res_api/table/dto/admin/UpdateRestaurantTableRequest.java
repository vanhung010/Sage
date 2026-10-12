package com.sagares.saga_res_api.table.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "Partial restaurant table update. At least one field must be supplied.")
public record UpdateRestaurantTableRequest(
        @Schema(description = "New unique table code. When supplied, it must be non-blank after trimming and at most 20 characters.", example = "T02", maxLength = 20, nullable = true)
        @Size(max = 20)
        String code,

        @Schema(description = "ID of the new active area.", example = "11", nullable = true)
        Long areaId,

        @Schema(description = "New minimum capacity. Must be greater than zero.", example = "2", minimum = "1", nullable = true)
        @Positive
        Short minCapacity,

        @Schema(description = "New maximum capacity. Must be greater than zero and not less than the resulting minCapacity.", example = "6", minimum = "1", nullable = true)
        @Positive
        Short maxCapacity
) {
}
