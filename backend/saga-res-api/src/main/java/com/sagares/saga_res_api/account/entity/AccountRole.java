package com.sagares.saga_res_api.account.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        description = "Account authorization role.",
        allowableValues = {"CUSTOMER", "STAFF", "ADMIN"},
        example = "CUSTOMER"
)
public enum AccountRole {
    CUSTOMER,
    STAFF,
    ADMIN
}
