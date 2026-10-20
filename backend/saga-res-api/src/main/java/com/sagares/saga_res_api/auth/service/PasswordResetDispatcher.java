package com.sagares.saga_res_api.auth.service;

import com.sagares.saga_res_api.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

@Component
@Slf4j
public class PasswordResetDispatcher {

    private final PasswordResetService passwordResetService;
    private final Executor executor;

    public PasswordResetDispatcher(
            PasswordResetService passwordResetService,
            @Qualifier("passwordResetExecutor") Executor executor
    ) {
        this.passwordResetService = passwordResetService;
        this.executor = executor;
    }

    public void submit(String email) {
        try {
            executor.execute(() -> {
                try {
                    passwordResetService.sendResetCode(email);
                } catch (Exception exception) {
                    log.error(
                            "Xử lý yêu cầu khôi phục mật khẩu thất bại: {}",
                            exception.getClass().getSimpleName()
                    );
                }
            });
        } catch (RejectedExecutionException exception) {
            throw new BusinessException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "PASSWORD_RESET_BUSY",
                    "Hệ thống đang bận. Vui lòng thử lại sau"
            );
        }
    }
}