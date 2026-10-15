package com.sagares.saga_res_api.account.dto;

import com.sagares.saga_res_api.account.entity.AccountRole;


public record CreateAaccountRequestDTO(
        String email,
        String password,
        String fullName,
        String phone,
        AccountRole role

) {
}
