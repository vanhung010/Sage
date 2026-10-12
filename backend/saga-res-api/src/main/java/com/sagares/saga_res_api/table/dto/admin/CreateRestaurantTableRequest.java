package com.sagares.saga_res_api.table.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateRestaurantTableRequest(
        @NotBlank
        @Size(max = 20)
        String code,

        @NotNull
        Long areaId,

        @NotNull
        @Positive
        Short minCapacity,

        @NotNull
        @Positive
        Short maxCapacity
) {
}
