package com.sagares.saga_res_api.table.dto.admin;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UpdateRestaurantTableRequest(
        @Size(max = 20)
        String code,

        Long areaId,

        @Positive
        Short minCapacity,

        @Positive
        Short maxCapacity
) {
}
