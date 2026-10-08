package com.sagares.saga_res_api.menu.dto.admin;

public record AdminCategoryResponse(
        Long id,
        String name,
        int displayOrder,
        boolean active
) {
}
