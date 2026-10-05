package com.sagares.saga_res_api.common.exception;

public record ErrorResponse(
        int status,
        String code,
        String message
) {
}
