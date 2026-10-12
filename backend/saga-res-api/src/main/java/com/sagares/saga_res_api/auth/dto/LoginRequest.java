package com.sagares.saga_res_api.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Credentials used to authenticate an existing account.")
public record LoginRequest(
        @Schema(description = "Account email. Must be non-blank and a valid email address.", example = "customer@example.com")
        @NotBlank @Email String email,

        @Schema(description = "Account password. Must be non-blank.", example = "Secret123")
        @NotBlank String password
) {
}
