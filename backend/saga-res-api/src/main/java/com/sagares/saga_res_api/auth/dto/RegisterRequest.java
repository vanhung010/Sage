package com.sagares.saga_res_api.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Customer self-registration request. The created account role is always CUSTOMER.")
public record RegisterRequest(

        @Schema(description = "Email address for the new account. Must be non-blank and valid.", example = "customer@example.com")
        @NotBlank
        @Email
        String email,

        @Schema(
                description = "Password with at least 8 characters, including at least one letter and one digit.",
                example = "Secret123",
                minLength = 8,
                pattern = "^(?=.*[A-Za-z])(?=.*\\d).+$"
        )
        @NotBlank
        @Size(min = 8)
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
                message = "Mật khẩu phải chứa ít nhất 1 chữ và 1 số"
        )
        String password,

        @Schema(description = "Customer's full name. Must be non-blank.", example = "Nguyen Van A")
        @NotBlank
        String fullName,

        @Schema(
                description = "Ten-digit Vietnamese phone number beginning with 0.",
                example = "0901234567",
                pattern = "^0[0-9]{9}$"
        )
        @NotBlank
        @Pattern(
                regexp = "^0[0-9]{9}$",
                message = "Số điện thoại phai chứa 10 chữ số và bắt đầu từ 0"
        )
        String phone
) {
}
