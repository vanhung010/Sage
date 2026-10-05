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
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        //Lấy token
        String token = header.substring(7);
        try {
            JwtService.JwtClaims claims = jwtService.parseAndValidate(token);
            Account account = accountRepository
                    .findById(claims.accountId())
                    .orElseThrow(() -> new BadCredentialsException("Invalid token"));
            if (account.isLocked()) {
                throw new BadCredentialsException("Invalid token");
            }
            if (account.getRole() != claims.role()) {
                throw new BadCredentialsException("Stale token");
            }
            //Tạo object đại diện cho user đang đăng nhập
            AuthenticatedUser principal = new AuthenticatedUser(account.getId(), account.getRole());
            //Tạo quyền/role cho user
            GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + account.getRole().name());
            //Tạo object Authentication cho Spring Security.
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(principal, null, List.of(authority));

            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            //Lưu authentication vào SecurityContextHolder
            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);

        } catch (JwtException | IllegalArgumentException | AuthenticationException e) {
            SecurityContextHolder.clearContext();
            authenticationEntryPoint.commence(request, response, new BadCredentialsException("Invalid access token", e));
        }
    }
}
