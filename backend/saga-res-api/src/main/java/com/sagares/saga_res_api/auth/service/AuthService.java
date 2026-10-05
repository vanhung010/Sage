package com.sagares.saga_res_api.auth.service;

import com.sagares.saga_res_api.account.entity.Account;
import com.sagares.saga_res_api.account.entity.AccountRole;
import com.sagares.saga_res_api.account.repository.AccountRepository;
import com.sagares.saga_res_api.auth.dto.*;
import com.sagares.saga_res_api.common.exception.BusinessException;
import com.sagares.saga_res_api.common.exception.UnauthorizedException;
import com.sagares.saga_res_api.security.JwtService;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public CurrentUserResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());

        if (accountRepository.existsByEmail(email)) {
            throw new BusinessException(
                    "Email đã tồn tại",
                    "Email đã được đăng kí"
            );
        }

        Account account = new Account();
        account.setEmail(email);
        account.setPasswordHash(
                passwordEncoder.encode(request.password())
        );
        account.setFullName(request.fullName().trim());
        account.setPhone(request.phone());
        account.setRole(AccountRole.CUSTOMER);
        account.setLocked(false);

        Account saved = accountRepository.save(account);

        return toCurrentUserResponse(saved);
    }

    public LoginResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());

        Account account = accountRepository.findByEmail(email)
                .orElseThrow(this::invalidCredentials);

        if (account.isLocked()) {
            throw new UnauthorizedException("Sai email hoặc mật khẩu");
        }

        if (!passwordEncoder.matches(
                request.password(),
                account.getPasswordHash()
        )) {
            throw invalidCredentials();
        }

        String token = jwtService.generateAccessToken(account);

        return new LoginResponse(token, "Bearer", jwtService.getExpiresInSeconds(),
                new AuthUserResponse(
                        account.getId(),
                        account.getFullName(),
                        account.getRole()
                )
        );
    }

    public CurrentUserResponse getCurrentUser(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() ->
                        new UnauthorizedException("Unauthorized"));
        if (account.isLocked()) {
            throw new UnauthorizedException("Unauthorized");
        }
        return toCurrentUserResponse(account);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private UnauthorizedException invalidCredentials() {
        return new UnauthorizedException("Sai email hoặc mật khẩu");
    }

    private CurrentUserResponse toCurrentUserResponse(Account account) {
        return new CurrentUserResponse(
                account.getId(),
                account.getEmail(),
                account.getFullName(),
                account.getPhone(),
                account.getRole()
        );
    }
}
