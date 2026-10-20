package com.sagares.saga_res_api.auth.service;

import com.sagares.saga_res_api.auth.config.PasswordResetProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

@Component
public class PasswordResetCrypto {

    private final SecureRandom random = new SecureRandom();
    private final SecretKeySpec hmacKey;

    public PasswordResetCrypto(PasswordResetProperties properties) {
        byte[] secret;

        try {
            secret = Base64.getDecoder().decode(properties.hmacSecret());
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "PASSWORD_RESET_HMAC_SECRET phải là Base64 hợp lệ"
            );
        }

        if (secret.length < 32) {
            throw new IllegalStateException(
                    "PASSWORD_RESET_HMAC_SECRET phải chứa ít nhất 32 byte"
            );
        }

        this.hmacKey = new SecretKeySpec(secret, "HmacSHA256");
    }

    public String generateCode() {
        return String.format(
                Locale.ROOT,
                "%06d",
                random.nextInt(1_000_000)
        );
    }

    public String generateResetToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);

        return HexFormat.of().formatHex(bytes);
    }

    public String hashCode(
            Long accountId,
            Instant createdAt,
            String code
    ) {
        // Gắn mã với tài khoản và thời điểm tạo yêu cầu.
        String payload = accountId
                + ":"
                + createdAt.toEpochMilli()
                + ":"
                + code;

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(hmacKey);

            return HexFormat.of().formatHex(
                    mac.doFinal(payload.getBytes(StandardCharsets.UTF_8))
            );
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(
                    "Không thể tạo HMAC cho mã xác nhận",
                    exception
            );
        }
    }

    public String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            return HexFormat.of().formatHex(
                    digest.digest(token.getBytes(StandardCharsets.UTF_8))
            );
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(
                    "Không thể băm token",
                    exception
            );
        }
    }

    public boolean matchesCode(String expectedHash, String actualHash) {
        return MessageDigest.isEqual(
                expectedHash.getBytes(StandardCharsets.UTF_8),
                actualHash.getBytes(StandardCharsets.UTF_8)
        );
    }
}