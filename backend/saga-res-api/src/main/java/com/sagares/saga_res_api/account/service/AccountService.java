package com.sagares.saga_res_api.account.service;

import com.sagares.saga_res_api.account.dto.AccountResponse;
import com.sagares.saga_res_api.account.dto.ChangePasswordRequest;
import com.sagares.saga_res_api.account.dto.CreateAaccountRequestDTO;
import com.sagares.saga_res_api.account.entity.Account;
import com.sagares.saga_res_api.account.repository.AccountRepository;
import com.sagares.saga_res_api.common.exception.BusinessException;
import com.sagares.saga_res_api.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountResponse create(CreateAaccountRequestDTO request) {
        String regexEmail = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        String regexPassword = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$";
        String regexPhone =  "^\\d+$";
        //Kiểm tra định dạng email
        //Kiểm tra trùng email
        //Kiểm tra password
        if(!request.email().matches(regexEmail)){
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "EMAIL NOT VALID",
                    "Email không hợp lệ");
        }

        if(!request.password().matches(regexPassword)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "PASSWORD NOT VALID",
                    "Password không hợp lệ");
        }

        if(!request.phone().trim().matches(regexPhone)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "Phone not valid",
                    "Số điện thoại không hợp lệ");
        }
        if(accountRepository.existsByEmail(request.email())){
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "EMAIL EXIST",
                    "Email đã tồn tại");
        }



        String emailNomal = normalizationString(request.email());

        //Tạo đối tượng Account
        Account account = new Account();

        account.setEmail(emailNomal);
        account.setPasswordHash(passwordEncoder.encode(request.password()));
        account.setFullName(request.fullName());
        account.setPhone(request.phone().trim());
        account.setRole(request.role());

        //Lưu
        Account accountSave = accountRepository.save(account);

        return toDTO(accountSave);


    }

    @Transactional
    public void changePassword(ChangePasswordRequest request, AuthenticatedUser users){
        if(!request.newPassword().equals(request.repeatPassword())){
            throw new BusinessException(HttpStatus.BAD_REQUEST, "PASSWORD NOT MATCH", "2 mật khẩu không khớp");
        }
        Account account = accountRepository.findByIdForUpdate(users.accountId()).orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                "ACCOUNT NOT FOUND", "Không tìm thấy tài khoản"));

        if(!passwordEncoder.matches(request.currentPassword(), account.getPasswordHash())){
            throw new BusinessException(HttpStatus.BAD_REQUEST, "PASSWORD NOT MATCH", "Mật khẩu hiện tại không khớp");
        }

        String passwordHash = passwordEncoder.encode(request.newPassword());

        account.setPasswordHash(passwordHash);

        accountRepository.save(account);
    }

    private static String normalizationString(String data){
        String result = data.toLowerCase().trim();
        return result;
    }
    private static AccountResponse toDTO(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getEmail(),
                account.getFullName(),
                account.getPhone(),
                account.getRole(),
                account.isLocked()
        );
    }

}
