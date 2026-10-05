package com.sagares.saga_res_api.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank
        @Email
        String email,

        @NotBlank
        @Size(min = 8)
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
                message = "Mật khẩu phải chứa ít nhất 1 chữ và 1 số"
        )
        String password,

        @NotBlank
        String fullName,

        @NotBlank
        @Pattern(
                regexp = "^0[0-9]{9}$",
                message = "Số điện thoại phai chứa 10 chữ số và bắt đầu từ 0"
        )
        String phone
) {
}
