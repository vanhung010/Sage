package com.sagares.saga_res_api.menu.controller;

import com.sagares.saga_res_api.account.repository.AccountRepository;
import com.sagares.saga_res_api.common.dto.PageResponse;
import com.sagares.saga_res_api.common.exception.BusinessException;
import com.sagares.saga_res_api.common.exception.GlobalExceptionHandler;
import com.sagares.saga_res_api.menu.dto.admin.AdminDishCategoryResponse;
import com.sagares.saga_res_api.menu.dto.admin.AdminDishResponse;
import com.sagares.saga_res_api.menu.dto.admin.AdminDishTagResponse;
import com.sagares.saga_res_api.menu.dto.admin.CreateDishRequest;
import com.sagares.saga_res_api.menu.dto.admin.UpdateDishRequest;
import com.sagares.saga_res_api.menu.dto.admin.UpdateDishStatusRequest;
import com.sagares.saga_res_api.menu.entity.DishStatus;
import com.sagares.saga_res_api.menu.service.AdminMenuDishService;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminMenuDishController.class)
@AutoConfigureMockMvc
@Import({
        SecurityConfig.class,
        GlobalExceptionHandler.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class,
        AdminMenuDishControllerTest.JwtFilterTestConfig.class
})
class AdminMenuDishControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminMenuDishService service;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private AccountRepository accountRepository;

    @TestConfiguration
    static class JwtFilterTestConfig {

        @Bean
        JwtAuthenticationFilter jwtAuthenticationFilter(
                JwtService jwtService,
                AccountRepository accountRepository,
                RestAuthenticationEntryPoint authenticationEntryPoint
        ) {
            return new JwtAuthenticationFilter(jwtService, accountRepository, authenticationEntryPoint);
        }
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanListAndFilterDishes() throws Exception {
        when(service.listDishes(1, 10, 5L, DishStatus.HIDDEN)).thenReturn(new PageResponse<>(
                List.of(response(DishStatus.HIDDEN)), 1, 10, 11, 2, false, true));

        mockMvc.perform(get("/api/admin/menu/dishes")
                        .param("page", "1")
                        .param("size", "10")
                        .param("categoryId", "5")
                        .param("status", "HIDDEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("HIDDEN"))
                .andExpect(jsonPath("$.content[0].category.name").value("Món chính"))
                .andExpect(jsonPath("$.totalElements").value(11));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanGetDishDetail() throws Exception {
        when(service.getDish(1L)).thenReturn(response(DishStatus.AVAILABLE));

        mockMvc.perform(get("/api/admin/menu/dishes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.category.id").value(5))
                .andExpect(jsonPath("$.tags[0].name").value("Đặc sắc"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createAcceptsMultipartFieldsAndImage() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "image", "dish.jpg", "image/jpeg", new byte[]{1, 2, 3});
        when(service.createDish(any(CreateDishRequest.class), eq(image)))
                .thenReturn(response(DishStatus.SOLD_OUT));

        mockMvc.perform(multipart("/api/admin/menu/dishes")
                        .file(image)
                        .param("categoryId", "5")
                        .param("name", "Cá kho")
                        .param("description", "Món Việt")
                        .param("price", "120000")
                        .param("status", "SOLD_OUT")
                        .param("displayOrder", "2")
                        .param("tagIds", "7", "8"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SOLD_OUT"));

        verify(service).createDish(
                eq(new CreateDishRequest(5L, "Cá kho", "Món Việt", 120000,
                        DishStatus.SOLD_OUT, 2, List.of(7L, 8L))),
                eq(image)
        );
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createWithoutImageIsSupported() throws Exception {
        when(service.createDish(any(CreateDishRequest.class), eq(null)))
                .thenReturn(response(DishStatus.AVAILABLE));

        mockMvc.perform(multipart("/api/admin/menu/dishes")
                        .param("categoryId", "5")
                        .param("name", "Cá kho")
                        .param("price", "120000"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.imageUrl").value("https://cdn.example/dish.jpg"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createValidationRejectsBlankNameAndNonPositivePrice() throws Exception {
        mockMvc.perform(multipart("/api/admin/menu/dishes")
                        .param("categoryId", "5")
                        .param("name", "   ")
                        .param("price", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateAcceptsMultipartPatchAndImage() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "image", "new.jpg", "image/jpeg", new byte[]{3, 2, 1});
        when(service.updateDish(eq(1L), any(UpdateDishRequest.class), eq(image)))
                .thenReturn(response(DishStatus.AVAILABLE));

        mockMvc.perform(multipart(HttpMethod.PATCH, "/api/admin/menu/dishes/{id}", 1L)
                        .file(image)
                        .param("name", "Tên mới")
                        .param("tagIds", "7", "8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(service).updateDish(
                eq(1L),
                eq(new UpdateDishRequest(null, "Tên mới", null, null, null, List.of(7L, 8L))),
                eq(image)
        );
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateBindsEmptyTagIdsAsAnExplicitEmptyList() throws Exception {
        when(service.updateDish(eq(1L), any(UpdateDishRequest.class), eq(null)))
                .thenReturn(response(DishStatus.AVAILABLE));

        mockMvc.perform(multipart(HttpMethod.PATCH, "/api/admin/menu/dishes/{id}", 1L)
                        .param("tagIds", ""))
                .andExpect(status().isOk());

        verify(service).updateDish(
                eq(1L),
                eq(new UpdateDishRequest(null, null, null, null, null, List.of())),
                eq(null)
        );
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void emptyMultipartPatchReturnsBusinessError() throws Exception {
        when(service.updateDish(eq(1L), any(UpdateDishRequest.class), eq(null)))
                .thenThrow(new BusinessException(
                        HttpStatus.BAD_REQUEST,
                        "EMPTY_PATCH",
                        "Phải cung cấp ít nhất một trường cần cập nhật."
                ));

        mockMvc.perform(multipart(HttpMethod.PATCH, "/api/admin/menu/dishes/{id}", 1L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EMPTY_PATCH"))
                .andExpect(jsonPath("$.message").value("Phải cung cấp ít nhất một trường cần cập nhật."));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanUpdateStatusWithJson() throws Exception {
        when(service.updateDishStatus(1L, new UpdateDishStatusRequest(DishStatus.HIDDEN)))
                .thenReturn(response(DishStatus.HIDDEN));

        mockMvc.perform(patch("/api/admin/menu/dishes/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"HIDDEN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("HIDDEN"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void nullStatusIsRejectedAtApiBoundary() throws Exception {
        mockMvc.perform(patch("/api/admin/menu/dishes/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("status: Trạng thái món ăn là bắt buộc."));
    }

    @Test
    void anonymousIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/menu/dishes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customerIsForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/menu/dishes"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void staffIsForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/menu/dishes"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    private AdminDishResponse response(DishStatus status) {
        return new AdminDishResponse(
                1L,
                "Cá kho",
                "Món Việt",
                120_000,
                "https://cdn.example/dish.jpg",
                status,
                2,
                new AdminDishCategoryResponse(5L, "Món chính"),
                List.of(new AdminDishTagResponse(7L, "Đặc sắc")),
                Instant.parse("2026-10-13T00:00:00Z"),
                Instant.parse("2026-10-13T01:00:00Z")
        );
    }
}
