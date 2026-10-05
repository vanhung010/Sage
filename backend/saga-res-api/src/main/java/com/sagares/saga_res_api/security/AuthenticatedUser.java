package com.sagares.saga_res_api.security;

import com.sagares.saga_res_api.account.entity.AccountRole;

public record AuthenticatedUser(
        Long accountId,
        AccountRole role
) {
}