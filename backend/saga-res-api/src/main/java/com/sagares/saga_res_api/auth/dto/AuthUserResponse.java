package com.sagares.saga_res_api.auth.dto;

import com.sagares.saga_res_api.account.entity.AccountRole;

public record AuthUserResponse(
        Long id,
        String fullName,
        AccountRole role
) {
}