package com.sagares.saga_res_api.auth.dto;

import com.sagares.saga_res_api.account.entity.AccountRole;

public record CurrentUserResponse(
        Long id,
        String email,
        String fullName,
        String phone,
        AccountRole role
) {
}
