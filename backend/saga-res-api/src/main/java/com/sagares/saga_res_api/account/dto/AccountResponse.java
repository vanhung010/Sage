package com.sagares.saga_res_api.account.dto;

import com.sagares.saga_res_api.account.entity.AccountRole;

public record AccountResponse(
        Long id,
        String email,
        String fullName,
        String phone,
        AccountRole role,
        boolean locked
) {
}
