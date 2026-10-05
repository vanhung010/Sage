package com.sagares.saga_res_api.auth.controller;

import com.sagares.saga_res_api.auth.dto.CurrentUserResponse;
import com.sagares.saga_res_api.auth.dto.LoginRequest;
import com.sagares.saga_res_api.auth.dto.LoginResponse;
import com.sagares.saga_res_api.auth.dto.RegisterRequest;
import com.sagares.saga_res_api.auth.service.AuthService;
import com.sagares.saga_res_api.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<CurrentUserResponse> register(@Valid @RequestBody RegisterRequest request) {
        CurrentUserResponse response = authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public CurrentUserResponse me(@AuthenticationPrincipal AuthenticatedUser user) {
        return authService.getCurrentUser(user.accountId());
    }
}