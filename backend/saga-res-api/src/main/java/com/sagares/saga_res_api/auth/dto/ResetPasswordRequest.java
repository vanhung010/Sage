package com.sagares.saga_res_api.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank
        @Pattern(
                regexp = "^[0-9a-f]{64}$",
                message = "Token đặt lại mật khẩu không hợp lệ"
        )
        String resetToken,

        @NotBlank
        @Size(min = 8, max = 72)
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
                message = "Mật khẩu phải chứa ít nhất 1 chữ và 1 số"
        )
        String newPassword,

        @NotBlank
        @Size(min = 8, max = 72)
        String confirmPassword
) {
}