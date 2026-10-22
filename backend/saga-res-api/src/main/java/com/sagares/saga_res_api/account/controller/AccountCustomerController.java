package com.sagares.saga_res_api.account.controller;

import com.sagares.saga_res_api.account.dto.ChangePasswordRequest;
import com.sagares.saga_res_api.account.service.AccountService;
import com.sagares.saga_res_api.security.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Null;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class AccountCustomerController {

    private final AccountService accountService;

    @PostMapping("/changePasswords")
    public ResponseEntity changePassword(@RequestBody @Valid ChangePasswordRequest request, @AuthenticationPrincipal AuthenticatedUser users){
        accountService.changePassword(request, users);
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
