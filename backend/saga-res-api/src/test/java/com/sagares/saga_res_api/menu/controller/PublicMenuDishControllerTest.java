package com.sagares.saga_res_api.menu.controller;

import com.sagares.saga_res_api.account.repository.AccountRepository;
import com.sagares.saga_res_api.common.dto.PageResponse;
import com.sagares.saga_res_api.common.exception.GlobalExceptionHandler;
import com.sagares.saga_res_api.menu.dto.publicmenu.DishSummaryResponse;
import com.sagares.saga_res_api.menu.dto.publicmenu.DishDetailResponse;
import com.sagares.saga_res_api.menu.dto.publicmenu.PublicDishCategoryResponse;
import com.sagares.saga_res_api.menu.service.PublicMenuDishService;
import com.sagares.saga_res_api.menu.entity.DishStatus;
import com.sagares.saga_res_api.security.JwtAuthenticationFilter;
import com.sagares.saga_res_api.security.JwtService;
import com.sagares.saga_res_api.security.RestAccessDeniedHandler;
import com.sagares.saga_res_api.security.RestAuthenticationEntryPoint;
import com.sagares.saga_res_api.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import io.jsonwebtoken.JwtException;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PublicMenuDishController.class)
@AutoConfigureMockMvc
@Import({SecurityConfig.class, GlobalExceptionHandler.class, RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class, PublicMenuDishControllerTest.JwtFilterTestConfig.class})
class PublicMenuDishControllerTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean PublicMenuDishService service;
    @MockitoBean JwtService jwtService;
    @MockitoBean AccountRepository accountRepository;

    @TestConfiguration
    static class JwtFilterTestConfig {
        @Bean JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService, AccountRepository accountRepository,
                                                               RestAuthenticationEntryPoint entryPoint) {
            return new JwtAuthenticationFilter(jwtService, accountRepository, entryPoint);
        }
    }




    @Test
    @org.springframework.security.test.context.support.WithMockUser(roles = "ADMIN")
    void detailIsReadableAndIncludesDisplayOrder() throws Exception {
        when(service.getDish(1L)).thenReturn(new DishDetailResponse(1L, "Soup", null, 120, null,
                DishStatus.AVAILABLE, new PublicDishCategoryResponse(2L, "Food", 1), List.of(), 4));
        mockMvc.perform(get("/api/menu/dishes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayOrder").value(4))
                .andExpect(jsonPath("$.tags").isArray());
    }

    @Test
    void anonymousDetailWorksButInvalidBearerIsStillRejectedByJwtFilter() throws Exception {
        when(service.getDish(1L)).thenReturn(new DishDetailResponse(1L, "Soup", null, 120, null,
                DishStatus.AVAILABLE, new PublicDishCategoryResponse(2L, "Food", 1), List.of(), 4));
        mockMvc.perform(get("/api/menu/dishes/1")).andExpect(status().isOk());
        org.mockito.Mockito.when(jwtService.parseAndValidate("invalid"))
                .thenThrow(new JwtException("invalid token"));
        mockMvc.perform(get("/api/menu/dishes/1").header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void malformedQueryAndPathAre400() throws Exception {
        mockMvc.perform(get("/api/menu/dishes").param("page", "x"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        mockMvc.perform(get("/api/menu/dishes/not-a-number"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }
}
