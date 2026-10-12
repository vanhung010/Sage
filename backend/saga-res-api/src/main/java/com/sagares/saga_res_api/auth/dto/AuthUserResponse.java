package com.sagares.saga_res_api.auth.dto;

import com.sagares.saga_res_api.account.entity.AccountRole;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Basic account information included with a successful login.")
public record AuthUserResponse(
        @Schema(description = "Unique account ID.", example = "12")
        Long id,

        @Schema(description = "Account holder's full name.", example = "Nguyen Van A")
        String fullName,

        @Schema(description = "Current account role.", example = "CUSTOMER")
        AccountRole role
) {
}
