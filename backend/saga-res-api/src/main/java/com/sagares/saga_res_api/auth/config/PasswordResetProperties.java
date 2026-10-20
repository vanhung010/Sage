package com.sagares.saga_res_api.auth.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.stream.Stream;

@Validated
@ConfigurationProperties(prefix = "app.password-reset")
public record PasswordResetProperties(
        Duration codeTtl,
        Duration tokenTtl,
        Duration resendCooldown,
        Duration requestWindow,

        @Positive
        int maxAttempts,

        @Positive
        int maxRequestsPerWindow,

        @NotBlank
        String hmacSecret
) {
    public PasswordResetProperties {
        boolean invalidDuration = Stream.of(
                codeTtl,
                tokenTtl,
                resendCooldown,
                requestWindow
        ).anyMatch(value ->
                value == null || value.isZero() || value.isNegative()
        );

        if (invalidDuration) {
            throw new IllegalArgumentException(
                    "Thời gian cấu hình password-reset phải lớn hơn 0"
            );
        }
    }
}