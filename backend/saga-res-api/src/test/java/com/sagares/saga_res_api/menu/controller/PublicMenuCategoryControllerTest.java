package com.sagares.saga_res_api.menu.controller;

import com.sagares.saga_res_api.account.repository.AccountRepository;
import com.sagares.saga_res_api.common.exception.GlobalExceptionHandler;
import com.sagares.saga_res_api.menu.entity.Category;
import com.sagares.saga_res_api.menu.repository.CategoryRepository;
import com.sagares.saga_res_api.menu.service.PublicMenuCategoryService;
import com.sagares.saga_res_api.security.JwtAuthenticationFilter;
import com.sagares.saga_res_api.security.JwtService;
import com.sagares.saga_res_api.security.RestAccessDeniedHandler;
import com.sagares.saga_res_api.security.RestAuthenticationEntryPoint;
import com.sagares.saga_res_api.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PublicMenuCategoryController.class)
@Import({PublicMenuCategoryService.class, SecurityConfig.class, GlobalExceptionHandler.class,
        RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class, JwtAuthenticationFilter.class})
class PublicMenuCategoryControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean CategoryRepository categoryRepository;
    @MockitoBean JwtService jwtService;
    @MockitoBean AccountRepository accountRepository;

    @Test
    void anonymousRequestReturnsOrderedArrayWithOnlyPublicFields() throws Exception {
        when(categoryRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc()).thenReturn(List.of(
                category(2L, "Khai vị", 1), category(5L, "Món chính", 2)));

        mockMvc.perform(get("/api/menu/categories"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0]", aMapWithSize(3)))
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].name").value("Khai vị"))
                .andExpect(jsonPath("$[0].displayOrder").value(1))
                .andExpect(jsonPath("$[1]", aMapWithSize(3)))
                .andExpect(jsonPath("$[1].id").value(5))
                .andExpect(jsonPath("$[1].name").value("Món chính"))
                .andExpect(jsonPath("$[1].displayOrder").value(2))
                .andExpect(jsonPath("$[*].active").doesNotExist());

        verify(categoryRepository).findByActiveTrueOrderByDisplayOrderAscNameAsc();
    }

    @Test
    void anonymousRequestReturnsEmptyArrayWhenNoActiveCategoriesExist() throws Exception {
        when(categoryRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc()).thenReturn(List.of());

        mockMvc.perform(get("/api/menu/categories"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("[]"));
    }

    private Category category(Long id, String name, int displayOrder) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setDisplayOrder(displayOrder);
        return category;
    }
}
