package com.sagares.saga_res_api.menu.controller;

import com.sagares.saga_res_api.account.repository.AccountRepository;
import com.sagares.saga_res_api.common.exception.GlobalExceptionHandler;
import com.sagares.saga_res_api.menu.entity.Tag;
import com.sagares.saga_res_api.menu.repository.TagRepository;
import com.sagares.saga_res_api.menu.service.PublicMenuTagService;
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

@WebMvcTest(PublicMenuTagController.class)
@Import({PublicMenuTagService.class, SecurityConfig.class, GlobalExceptionHandler.class,
        RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class, JwtAuthenticationFilter.class})
class PublicMenuTagControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean TagRepository tagRepository;
    @MockitoBean JwtService jwtService;
    @MockitoBean AccountRepository accountRepository;

    @Test
    void anonymousRequestReturnsOrderedArrayWithOnlyIdAndName() throws Exception {
        when(tagRepository.findAllByOrderByNameAsc()).thenReturn(List.of(
                tag(1L, "Bán chạy"), tag(2L, "Món mới")));

        mockMvc.perform(get("/api/menu/tags"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0]", aMapWithSize(2)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Bán chạy"))
                .andExpect(jsonPath("$[1]", aMapWithSize(2)))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].name").value("Món mới"));

        verify(tagRepository).findAllByOrderByNameAsc();
    }

    @Test
    void anonymousRequestReturnsEmptyArrayWhenNoTagsExist() throws Exception {
        when(tagRepository.findAllByOrderByNameAsc()).thenReturn(List.of());

        mockMvc.perform(get("/api/menu/tags"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("[]"));
    }

    private Tag tag(Long id, String name) {
        Tag tag = new Tag();
        tag.setId(id);
        tag.setName(name);
        return tag;
    }
}
