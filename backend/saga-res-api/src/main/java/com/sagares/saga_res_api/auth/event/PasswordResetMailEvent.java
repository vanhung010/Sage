package com.sagares.saga_res_api.auth.event;

public record PasswordResetMailEvent(
        String recipient,
        String subject,
        String body
) {
}