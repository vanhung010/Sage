package com.sagares.saga_res_api.auth.controller;

import com.sagares.saga_res_api.auth.dto.ForgotPasswordRequest;
import com.sagares.saga_res_api.auth.dto.PasswordResetMessageResponse;
import com.sagares.saga_res_api.auth.dto.ResetPasswordRequest;
import com.sagares.saga_res_api.auth.dto.VerifyResetCodeRequest;
import com.sagares.saga_res_api.auth.dto.VerifyResetCodeResponse;
import com.sagares.saga_res_api.auth.service.PasswordResetDispatcher;
import com.sagares.saga_res_api.auth.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetDispatcher dispatcher;
    private final PasswordResetService passwordResetService;

    @PostMapping("/forgot-password")
    public ResponseEntity<PasswordResetMessageResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request
    ) {
        dispatcher.submit(request.email());

        return ResponseEntity.accepted().body(
                new PasswordResetMessageResponse(
                        "Nếu email này đã được đăng ký và tài khoản đủ điều kiện, "
                                + "bạn sẽ nhận được mã xác nhận."
                )
        );
    }

    @PostMapping("/verify-reset-code")
    public VerifyResetCodeResponse verifyResetCode(
            @Valid @RequestBody VerifyResetCodeRequest request
    ) {
        return passwordResetService.verifyResetCode(request);
    }

    @PostMapping("/reset-password")
    public PasswordResetMessageResponse resetPassword(
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        passwordResetService.resetPassword(request);

        return new PasswordResetMessageResponse(
                "Đặt lại mật khẩu thành công. Vui lòng đăng nhập."
        );
    }
}