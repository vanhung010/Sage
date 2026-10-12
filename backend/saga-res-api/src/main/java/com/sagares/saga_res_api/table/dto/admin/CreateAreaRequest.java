package com.sagares.saga_res_api.table.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAreaRequest(
        @NotBlank
        @Size(max = 100)
        String name,

        String description
) {
}
