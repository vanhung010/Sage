package com.sagares.saga_res_api.security;

import com.sagares.saga_res_api.account.entity.Account;
import com.sagares.saga_res_api.account.repository.AccountRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;


import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AccountRepository accountRepository;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        try {
            JwtService.JwtClaims claims =
                    jwtService.parseAndValidate(token);

            Account account = accountRepository
                    .findById(claims.accountId())
                    .orElseThrow(() ->
                            new BadCredentialsException("Invalid token")
                    );

            if (account.isLocked()) {
                throw new BadCredentialsException("Account locked");
            }

            if (account.getRole() != claims.role()) {
                throw new BadCredentialsException("Stale token");
            }

            AuthenticatedUser principal = new AuthenticatedUser(
                    account.getId(),
                    account.getRole()
            );

            GrantedAuthority authority = new SimpleGrantedAuthority(
                    "ROLE_" + account.getRole().name()
            );

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            List.of(authority)
                    );

            authentication.setDetails(
                    new WebAuthenticationDetailsSource()
                            .buildDetails(request)
            );

            SecurityContextHolder.getContext()
                    .setAuthentication(authentication);

        } catch (
                JwtException
                | IllegalArgumentException
                | AuthenticationException exception
        ) {
            SecurityContextHolder.clearContext();

            authenticationEntryPoint.commence(
                    request,
                    response,
                    new BadCredentialsException(
                            "Invalid access token",
                            exception
                    )
            );

            return;
        }

        // Đặt ngoài try/catch JWT để lỗi tạo đơn được xử lý đúng.
        filterChain.doFilter(request, response);
    }
}
