package com.sagares.saga_res_api.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(

        @NotBlank(message = "Vui lòng nhập mật khẩu mới")
        @Size(
                min = 8,
                max = 72,
                message = "Mật khẩu mới phải có từ 8 đến 72 ký tự"
        )
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
                message = "Mật khẩu mới phải chứa ít nhất một chữ cái và một chữ số"
        )
        String newPassword,

        @NotBlank(message = "Vui lòng nhập mật khẩu hiện tại")
        String currentPassword,

        @NotBlank(message = "Vui lòng nhập lại mật khẩu mới")
        @Size(
                min = 8,
                max = 72,
                message = "Mật khẩu xác nhận phải có từ 8 đến 72 ký tự"
        )
        String repeatPassword

) {
}