package com.sagares.saga_res_api.auth.exception;

import com.sagares.saga_res_api.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class InvalidResetCodeException extends BusinessException {

    public InvalidResetCodeException() {
        super(
                HttpStatus.BAD_REQUEST,
                "INVALID_RESET_CODE",
                "Mã xác nhận không hợp lệ hoặc đã hết hạn"
        );
    }
}