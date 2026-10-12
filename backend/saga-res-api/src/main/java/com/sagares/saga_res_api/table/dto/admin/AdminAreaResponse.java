package com.sagares.saga_res_api.table.dto.admin;

public record AdminAreaResponse(
        Long id,
        String name,
        String description,
        boolean active
) {
}
