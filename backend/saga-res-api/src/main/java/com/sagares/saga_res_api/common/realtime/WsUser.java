package com.sagares.saga_res_api.common.realtime;

import com.sagares.saga_res_api.account.entity.AccountRole;

import java.security.Principal;
import java.time.Instant;

public record WsUser(
        Long accountId,
        AccountRole role,
        Instant expiresAt
) implements Principal {
    @Override
    public String getName() {
        return accountId.toString();
    }
}