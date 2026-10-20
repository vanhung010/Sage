package com.sagares.saga_res_api.account.controller;

import com.sagares.saga_res_api.account.service.AccountService;
import com.sagares.saga_res_api.account.dto.AccountResponse;
import com.sagares.saga_res_api.account.dto.CreateAaccountRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/acocunt")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(@RequestBody CreateAaccountRequestDTO request) {
        AccountResponse data = accountService.create(request);
        return ResponseEntity.ok(data);
    }
}
