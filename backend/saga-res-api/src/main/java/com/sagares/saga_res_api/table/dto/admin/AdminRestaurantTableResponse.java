package com.sagares.saga_res_api.table.dto.admin;

public record AdminRestaurantTableResponse(
        Long id,
        String code,
        AdminTableAreaResponse area,
        Short minCapacity,
        Short maxCapacity,
        boolean active
) {
}
