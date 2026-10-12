package com.sagares.saga_res_api.menu.controller;

import com.sagares.saga_res_api.account.repository.AccountRepository;
import com.sagares.saga_res_api.common.exception.BusinessException;
import com.sagares.saga_res_api.common.exception.GlobalExceptionHandler;
import com.sagares.saga_res_api.menu.dto.admin.AdminTagResponse;
import com.sagares.saga_res_api.menu.dto.admin.CreateTagRequest;
import com.sagares.saga_res_api.menu.dto.admin.UpdateTagRequest;
import com.sagares.saga_res_api.menu.service.AdminMenuTagService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminMenuTagController.class)
@AutoConfigureMockMvc
@Import({
        SecurityConfig.class,
        GlobalExceptionHandler.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class,
        AdminMenuTagControllerTest.JwtFilterTestConfig.class
})
class AdminMenuTagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminMenuTagService service;

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
    void adminCanListTagsInServiceOrder() throws Exception {
        when(service.listTags()).thenReturn(List.of(
                new AdminTagResponse(2L, "Spicy"),
                new AdminTagResponse(1L, "Vegetarian")
        ));

        mockMvc.perform(get("/api/admin/menu/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].name").value("Spicy"))
                .andExpect(jsonPath("$[1].id").value(1))
                .andExpect(jsonPath("$[1].name").value("Vegetarian"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanCreateTag() throws Exception {
        when(service.createTag(any(CreateTagRequest.class)))
                .thenReturn(new AdminTagResponse(1L, "Spicy"));

        mockMvc.perform(post("/api/admin/menu/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Spicy\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Spicy"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createBlankNameIsRejectedAtApiBoundary() throws Exception {
        mockMvc.perform(post("/api/admin/menu/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("name: Tên nhãn không được để trống"));
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createTooLongNameIsRejectedAtApiBoundary() throws Exception {
        mockMvc.perform(post("/api/admin/menu/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"%s\"}".formatted("a".repeat(51))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("name: Tên nhãn không được vượt quá 50 ký tự"));
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void duplicateCreateReturnsVietnameseConflict() throws Exception {
        when(service.createTag(any(CreateTagRequest.class))).thenThrow(new BusinessException(
                HttpStatus.CONFLICT, "DUPLICATE_TAG_NAME", "Tên nhãn đã tồn tại"));

        mockMvc.perform(post("/api/admin/menu/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Spicy\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_TAG_NAME"))
                .andExpect(jsonPath("$.message").value("Tên nhãn đã tồn tại"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanUpdateTag() throws Exception {
        when(service.updateTag(eq(1L), any(UpdateTagRequest.class)))
                .thenReturn(new AdminTagResponse(1L, "Very Spicy"));

        mockMvc.perform(patch("/api/admin/menu/tags/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Very Spicy\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Very Spicy"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateUnknownTagReturnsNotFound() throws Exception {
        when(service.updateTag(eq(999L), any(UpdateTagRequest.class))).thenThrow(new BusinessException(
                HttpStatus.NOT_FOUND, "TAG_NOT_FOUND", "Nhãn không tồn tại"));

        mockMvc.perform(patch("/api/admin/menu/tags/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Spicy\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TAG_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Nhãn không tồn tại"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void emptyPatchReturnsSpecifiedBusinessError() throws Exception {
        when(service.updateTag(eq(1L), any(UpdateTagRequest.class))).thenThrow(new BusinessException(
                HttpStatus.BAD_REQUEST, "EMPTY_PATCH", "Phải cung cấp ít nhất một trường để cập nhật"));

        mockMvc.perform(patch("/api/admin/menu/tags/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EMPTY_PATCH"))
                .andExpect(jsonPath("$.message").value("Phải cung cấp ít nhất một trường để cập nhật"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanDeleteUnusedTagWithNoContentResponse() throws Exception {
        doNothing().when(service).deleteTag(1L);

        mockMvc.perform(delete("/api/admin/menu/tags/1"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(service).deleteTag(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteTagInUseReturnsVietnameseConflict() throws Exception {
        doThrow(new BusinessException(
                HttpStatus.CONFLICT,
                "TAG_IN_USE",
                "Không thể xóa nhãn đang được gán cho món ăn"
        )).when(service).deleteTag(1L);

        mockMvc.perform(delete("/api/admin/menu/tags/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TAG_IN_USE"))
                .andExpect(jsonPath("$.message").value("Không thể xóa nhãn đang được gán cho món ăn"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteUnknownTagReturnsNotFound() throws Exception {
        doThrow(new BusinessException(HttpStatus.NOT_FOUND, "TAG_NOT_FOUND", "Nhãn không tồn tại"))
                .when(service).deleteTag(999L);

        mockMvc.perform(delete("/api/admin/menu/tags/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TAG_NOT_FOUND"));
    }
}
