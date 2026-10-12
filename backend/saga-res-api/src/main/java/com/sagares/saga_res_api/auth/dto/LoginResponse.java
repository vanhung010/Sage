package com.sagares.saga_res_api.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Successful authentication result containing a short-lived JWT access token and basic account details.")
public record LoginResponse(
        @Schema(
                description = "JWT access token for the Authorization header. The example is intentionally not a usable token.",
                example = "eyJhbGciOiJIUzI1NiJ9.example.signature"
        )
        String accessToken,

        @Schema(description = "Authorization scheme to use with the token.", example = "Bearer")
        String tokenType,

        @Schema(description = "Token lifetime in seconds.", example = "3600")
        long expiresIn,

        @Schema(description = "Authenticated account summary.")
        AuthUserResponse user
) {
}
