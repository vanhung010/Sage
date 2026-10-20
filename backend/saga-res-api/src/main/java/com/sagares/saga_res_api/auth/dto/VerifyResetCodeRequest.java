package com.sagares.saga_res_api.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record VerifyResetCodeRequest(
        @NotBlank
        @Email
        @Size(max = 255)
        String email,

        @NotBlank
        @Pattern(
                regexp = "^[0-9]{6}$",
                message = "Mã xác nhận phải gồm 6 chữ số"
        )
        String code
) {
}