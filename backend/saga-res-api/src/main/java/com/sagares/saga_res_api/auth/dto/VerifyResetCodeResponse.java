package com.sagares.saga_res_api.auth.dto;

public record VerifyResetCodeResponse(
        String resetToken,
        long expiresInSeconds
) {
}