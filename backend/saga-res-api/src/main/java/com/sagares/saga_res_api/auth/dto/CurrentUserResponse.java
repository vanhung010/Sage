package com.sagares.saga_res_api.auth.dto;

import com.sagares.saga_res_api.account.entity.AccountRole;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Current account profile returned after registration or for the authenticated account.")
public record CurrentUserResponse(
        @Schema(description = "Unique account ID.", example = "12")
        Long id,

        @Schema(description = "Normalized account email address.", example = "customer@example.com")
        String email,

        @Schema(description = "Account holder's full name.", example = "Nguyen Van A")
        String fullName,

        @Schema(description = "Ten-digit Vietnamese phone number beginning with 0.", example = "0901234567")
        String phone,

        @Schema(description = "Current account role.", example = "CUSTOMER")
        AccountRole role
) {
}
