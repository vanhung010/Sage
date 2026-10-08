package com.sagares.saga_res_api.menu.controller;

import com.sagares.saga_res_api.account.repository.AccountRepository;
import com.sagares.saga_res_api.common.exception.BusinessException;
import com.sagares.saga_res_api.common.exception.GlobalExceptionHandler;
import com.sagares.saga_res_api.menu.dto.admin.AdminCategoryResponse;
import com.sagares.saga_res_api.menu.dto.admin.CreateCategoryRequest;
import com.sagares.saga_res_api.menu.dto.admin.UpdateCategoryRequest;
import com.sagares.saga_res_api.menu.service.AdminMenuCategoryService;
import com.sagares.saga_res_api.security.JwtAuthenticationFilter;
import com.sagares.saga_res_api.security.JwtService;
import com.sagares.saga_res_api.security.RestAccessDeniedHandler;
import com.sagares.saga_res_api.security.RestAuthenticationEntryPoint;
import com.sagares.saga_res_api.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminMenuCategoryController.class)
@AutoConfigureMockMvc
@Import({
        SecurityConfig.class,
        GlobalExceptionHandler.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class,
        AdminMenuCategoryControllerTest.JwtFilterTestConfig.class
})
class AdminMenuCategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminMenuCategoryService adminMenuCategoryService;

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
    void adminCanListCategoriesIncludingInactiveInServiceOrder() throws Exception {
        when(adminMenuCategoryService.listCategories()).thenReturn(List.of(
                new AdminCategoryResponse(2L, "A", 1, false),
                new AdminCategoryResponse(1L, "B", 1, true)
        ));

        mockMvc.perform(get("/api/admin/menu/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].name").value("A"))
                .andExpect(jsonPath("$[0].displayOrder").value(1))
                .andExpect(jsonPath("$[0].active").value(false))
                .andExpect(jsonPath("$[1].active").value(true));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanCreateCategory() throws Exception {
        when(adminMenuCategoryService.createCategory(any(CreateCategoryRequest.class)))
                .thenReturn(new AdminCategoryResponse(1L, "Khai vị", 1, true));

        mockMvc.perform(post("/api/admin/menu/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Khai vị",
                                  "displayOrder": 1
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Khai vị"))
                .andExpect(jsonPath("$.displayOrder").value(1))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void postBlankNameReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/admin/menu/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "   ",
                                  "displayOrder": 1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verifyNoInteractions(adminMenuCategoryService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void postNegativeDisplayOrderReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/admin/menu/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Khai vị",
                                  "displayOrder": -1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void postDuplicateCategoryNameReturnsConflict() throws Exception {
        when(adminMenuCategoryService.createCategory(any(CreateCategoryRequest.class)))
                .thenThrow(new BusinessException(
                        HttpStatus.CONFLICT,
                        "DUPLICATE_CATEGORY_NAME",
                        "Category name already exists."
                ));

        mockMvc.perform(post("/api/admin/menu/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Khai vị",
                                  "displayOrder": 1
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_CATEGORY_NAME"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void patchReturnsUpdatedCategory() throws Exception {
        when(adminMenuCategoryService.updateCategory(eq(1L), any(UpdateCategoryRequest.class)))
                .thenReturn(new AdminCategoryResponse(1L, "Món khai vị", 2, false));

        mockMvc.perform(patch("/api/admin/menu/categories/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Món khai vị",
                                  "displayOrder": 2,
                                  "active": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Món khai vị"))
                .andExpect(jsonPath("$.displayOrder").value(2))
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void patchUnknownIdReturnsNotFound() throws Exception {
        when(adminMenuCategoryService.updateCategory(eq(999L), any(UpdateCategoryRequest.class)))
                .thenThrow(new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "CATEGORY_NOT_FOUND",
                        "Category not found."
                ));

        mockMvc.perform(patch("/api/admin/menu/categories/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Món khai vị"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CATEGORY_NOT_FOUND"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void patchDuplicateNameReturnsConflict() throws Exception {
        when(adminMenuCategoryService.updateCategory(eq(1L), any(UpdateCategoryRequest.class)))
                .thenThrow(new BusinessException(
                        HttpStatus.CONFLICT,
                        "DUPLICATE_CATEGORY_NAME",
                        "Category name already exists."
                ));

        mockMvc.perform(patch("/api/admin/menu/categories/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Món khai vị"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_CATEGORY_NAME"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void emptyPatchReturnsBadRequest() throws Exception {
        when(adminMenuCategoryService.updateCategory(eq(1L), any(UpdateCategoryRequest.class)))
                .thenThrow(new BusinessException(
                        HttpStatus.BAD_REQUEST,
                        "EMPTY_PATCH",
                        "At least one category field must be supplied."
                ));

        mockMvc.perform(patch("/api/admin/menu/categories/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EMPTY_PATCH"));
    }

    @Test
    void anonymousAccessReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/menu/categories"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customerAccessReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/menu/categories"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("permission")));
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void staffAccessReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/menu/categories"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }
}
