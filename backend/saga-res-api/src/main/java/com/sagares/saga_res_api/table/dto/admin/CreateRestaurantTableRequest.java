package com.sagares.saga_res_api.table.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to create a restaurant table in an active area.")
public record CreateRestaurantTableRequest(
        @Schema(description = "Unique table code. Must be non-blank and at most 20 characters.", example = "T01", maxLength = 20)
        @NotBlank
        @Size(max = 20)
        String code,

        @Schema(description = "ID of an existing active area.", example = "10")
        @NotNull
        Long areaId,

        @Schema(description = "Minimum supported party size. Must be greater than zero.", example = "2", minimum = "1")
        @NotNull
        @Positive
        Short minCapacity,

        @Schema(description = "Maximum supported party size. Must be greater than zero and not less than minCapacity.", example = "4", minimum = "1")
        @NotNull
        @Positive
        Short maxCapacity
) {
}
