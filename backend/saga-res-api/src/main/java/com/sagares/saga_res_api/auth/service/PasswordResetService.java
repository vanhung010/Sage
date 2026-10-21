package com.sagares.saga_res_api.auth.service;

import com.sagares.saga_res_api.account.entity.Account;
import com.sagares.saga_res_api.account.repository.AccountRepository;
import com.sagares.saga_res_api.auth.config.PasswordResetProperties;
import com.sagares.saga_res_api.auth.dto.ResetPasswordRequest;
import com.sagares.saga_res_api.auth.dto.VerifyResetCodeRequest;
import com.sagares.saga_res_api.auth.dto.VerifyResetCodeResponse;
import com.sagares.saga_res_api.auth.entity.PasswordResetRequest;
import com.sagares.saga_res_api.auth.event.PasswordResetMailEvent;
import com.sagares.saga_res_api.auth.exception.InvalidResetCodeException;
import com.sagares.saga_res_api.auth.repository.PasswordResetRequestRepository;
import com.sagares.saga_res_api.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PasswordResetService {
    private final AccountRepository accountRepository;
    private final PasswordResetRequestRepository resetRepository;
    private final PasswordResetProperties properties;
    private final PasswordResetCrypto crypto;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    //Xử lí yêu cầu tạo và gửi mã
    @Transactional
    public void sendResetCode(String email){
        Account account = accountRepository
                .findByEmailForUpdate(normalizeEmail(email))
                .orElse(null);


        if( account == null || account.isLocked()){
            return;
        }

        Instant now = Instant.now();

        var latestRequest = resetRepository.findFirstByAccount_IdOrderByCreatedAtDesc(account.getId());

        // Yêu cầu gửi lại quá sớm vẫn nhận cùng thông báo chung.
        if (latestRequest.isPresent()
                && latestRequest.get().getCreatedAt()
                .plus(properties.resendCooldown())
                .isAfter(now)) {
            return;
        }
        long requestCount = resetRepository
                .countByAccount_IdAndCreatedAtAfter(
                        account.getId(),
                        now.minus(properties.requestWindow())
                );

        if (requestCount >= properties.maxRequestsPerWindow()) {
            return;
        }

        resetRepository
                .findByAccount_IdAndUsedAtIsNullAndRevokedAtIsNull(
                        account.getId()
                )
                .ifPresent(request -> request.setRevokedAt(now));

        resetRepository.flush();

        String code = crypto.generateCode();

        PasswordResetRequest request = new PasswordResetRequest();

        request.setAccount(account);
        request.setCreatedAt(now);
        request.setCodeExpiresAt(now.plus(properties.codeTtl()));
        request.setFailedAttempts(0);
        request.setCodeHash(
                crypto.hashCode(account.getId(), now, code)
        );

        resetRepository.save(request);

        long minutes = Math.max(
                5,
                (properties.codeTtl().toSeconds() + 59) / 60
        );

        eventPublisher.publishEvent(new PasswordResetMailEvent(
                account.getEmail(),
                "Sage – Mã xác nhận đặt lại mật khẩu",
                """
                Mã xác nhận của bạn: %s

                Mã có hiệu lực trong %d phút.
                Không chia sẻ mã này với bất kỳ ai.

                Nếu bạn không yêu cầu đặt lại mật khẩu,
                hãy bỏ qua email này.
                """.formatted(code, minutes)
        ));
    }

    // API 2: kiểm tra mã rồi cấp resetToken.
    @Transactional(noRollbackFor = InvalidResetCodeException.class)
    public VerifyResetCodeResponse verifyResetCode(
            VerifyResetCodeRequest input) {
        Account account = accountRepository
                .findByEmailForUpdate(normalizeEmail(input.email()))
                .orElseThrow(InvalidResetCodeException::new);

        if (account.isLocked()) {
            throw new InvalidResetCodeException();
        }

        PasswordResetRequest request = resetRepository.findByAccount_IdAndUsedAtIsNullAndRevokedAtIsNull(account.getId())
                .orElseThrow(InvalidResetCodeException::new);

        // Mã chỉ được xác thực thành công một lần.
        // Không làm mất hiệu lực resetToken đang có khi mã bị gửi lại.
        if (request.getVerifiedAt() != null) {
            throw new InvalidResetCodeException();
        }

        Instant now = now();
        //Hết hạn hoặc nhập sai quas số lần
        if (!now.isBefore(request.getCodeExpiresAt())
                || request.getFailedAttempts() >= properties.maxAttempts()) {
            request.setRevokedAt(now);
            throw new InvalidResetCodeException();
        }

        String submittedHash = crypto.hashCode(
                account.getId(),
                request.getCreatedAt(),
                input.code()
        );

        if (!crypto.matchesCode(request.getCodeHash(), submittedHash)) {
            int attempts = request.getFailedAttempts() + 1;

            request.setFailedAttempts(attempts);

            if (attempts >= properties.maxAttempts()) {
                request.setRevokedAt(now);
            }

            // noRollbackFor giúp lưu bộ đếm và trạng thái thu hồi.
            throw new InvalidResetCodeException();
        }

        String resetToken = crypto.generateResetToken();

        request.setVerifiedAt(now);
        request.setResetTokenHash(crypto.hashToken(resetToken));
        request.setResetTokenExpiresAt(now.plus(properties.tokenTtl()));

        return new VerifyResetCodeResponse(
                resetToken,
                properties.tokenTtl().toSeconds()
        );
    }


    // API 3: sử dụng resetToken để cập nhật mật khẩu.
    @Transactional
    public void resetPassword(ResetPasswordRequest input) {
        if (!input.newPassword().equals(input.confirmPassword())) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "PASSWORD_CONFIRMATION_MISMATCH",
                    "Mật khẩu xác nhận không khớp"
            );
        }

        // BCrypt giới hạn theo byte, không chỉ theo số ký tự.
        if (input.newPassword()
                .getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "PASSWORD_TOO_LONG",
                    "Mật khẩu vượt quá giới hạn 72 byte"
            );
        }

        String tokenHash = crypto.hashToken(input.resetToken());

        // Chỉ lấy accountId trước, chưa tải entity request vào persistence context.
        Long accountId = resetRepository
                .findAccountIdByTokenHash(tokenHash)
                .orElseThrow(this::invalidResetToken);

        // Cùng thứ tự khóa với hai API trước: account trước.
        Account account = accountRepository
                .findByIdForUpdate(accountId)
                .orElseThrow(this::invalidResetToken);

        if (account.isLocked()) {
            throw invalidResetToken();
        }

        // Đọc trạng thái mới nhất sau khi đã lấy khóa account.
        PasswordResetRequest request = resetRepository
                .findByResetTokenHash(tokenHash)
                .orElseThrow(this::invalidResetToken);

        Instant now = now();

        if (request.getVerifiedAt() == null
                || request.getUsedAt() != null
                || request.getRevokedAt() != null
                || request.getResetTokenExpiresAt() == null
                || !now.isBefore(request.getResetTokenExpiresAt())) {
            throw invalidResetToken();
        }

        account.setPasswordHash(
                passwordEncoder.encode(input.newPassword())
        );

        request.setUsedAt(now);

        // Hai entity đang được quản lý bởi JPA.
        // Mật khẩu và usedAt được lưu trong cùng transaction.
        eventPublisher.publishEvent(new PasswordResetMailEvent(
                account.getEmail(),
                "Sage – Mật khẩu của bạn đã được thay đổi",
                """
                Mật khẩu tài khoản Sage của bạn vừa được đặt lại.

                Nếu bạn thực hiện thao tác này, bạn có thể đăng nhập
                bằng mật khẩu mới.

                Nếu không phải bạn thực hiện, vui lòng liên hệ Sage.
                """
        ));
    }


    private BusinessException invalidResetToken() {
        return new BusinessException(
                HttpStatus.BAD_REQUEST,
                "INVALID_RESET_TOKEN",
                "Yêu cầu đặt lại mật khẩu không hợp lệ hoặc đã hết hạn"
        );
    }

    private Instant now() {
        // Giữ độ chính xác nhất quán khi lưu và đọc lại từ PostgreSQL.
        return Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
