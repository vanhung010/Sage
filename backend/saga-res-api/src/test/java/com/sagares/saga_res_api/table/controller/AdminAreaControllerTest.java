package com.sagares.saga_res_api.table.controller;

import com.sagares.saga_res_api.account.entity.Account;
import com.sagares.saga_res_api.account.entity.AccountRole;
import com.sagares.saga_res_api.account.repository.AccountRepository;
import com.sagares.saga_res_api.common.exception.GlobalExceptionHandler;
import com.sagares.saga_res_api.security.JwtAuthenticationFilter;
import com.sagares.saga_res_api.security.JwtService;
import com.sagares.saga_res_api.security.RestAccessDeniedHandler;
import com.sagares.saga_res_api.security.RestAuthenticationEntryPoint;
import com.sagares.saga_res_api.security.SecurityConfig;
import com.sagares.saga_res_api.table.entity.Area;
import com.sagares.saga_res_api.table.repository.AreaRepository;
import com.sagares.saga_res_api.table.service.AdminAreaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminAreaController.class)
@AutoConfigureMockMvc
@WithMockUser(roles = "ADMIN")
@Import({
        SecurityConfig.class,
        GlobalExceptionHandler.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class,
        AdminAreaService.class,
        AdminAreaControllerTest.JwtFilterTestConfig.class
})
class AdminAreaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AreaRepository areaRepository;

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
    void adminCanListAreasIncludingInactiveInNameOrder() throws Exception {
        when(areaRepository.findAllByOrderByNameAsc()).thenReturn(List.of(
                area(2L, "Garden", "Outdoor seating", false),
                area(1L, "Indoor", "Main dining room", true)
        ));

        mockMvc.perform(get("/api/admin/areas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].name").value("Garden"))
                .andExpect(jsonPath("$[0].description").value("Outdoor seating"))
                .andExpect(jsonPath("$[0].active").value(false))
                .andExpect(jsonPath("$[1].name").value("Indoor"))
                .andExpect(jsonPath("$[1].active").value(true));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Indoor", "Garden"})
    void adminCanCreateAreaWithActiveDefaultAndTrimmedText(String name) throws Exception {
        when(areaRepository.saveAndFlush(any(Area.class))).thenAnswer(invocation -> {
            Area saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        mockMvc.perform(post("/api/admin/areas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "  %s  ", "description": "  Dining area  "}
                                """.formatted(name)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.description").value("Dining area"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void createDoesNotAllowClientToChooseIdOrActive() throws Exception {
        when(areaRepository.saveAndFlush(any(Area.class))).thenAnswer(invocation -> {
            Area saved = invocation.getArgument(0);
            assertThat(saved.getId()).isNull();
            saved.setId(1L);
            return saved;
        });

        mockMvc.perform(post("/api/admin/areas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id": 999, "name": "Indoor", "active": false}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.active").value(true));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"name\": null}", "{\"name\": \"\"}", "{\"name\": \"   \"}"})
    void createRejectsMissingOrBlankName(String body) throws Exception {
        mockMvc.perform(post("/api/admin/areas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        verifyNoInteractions(areaRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PATCH"})
    void namesLongerThan100CharactersAreRejected(String operation) throws Exception {
        MockHttpServletRequestBuilder request = operation.equals("POST")
                ? post("/api/admin/areas") : patch("/api/admin/areas/1");

        mockMvc.perform(request.contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"" + "a".repeat(101) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        verifyNoInteractions(areaRepository);
    }

    @Test
    void duplicateCreateReturnsConflict() throws Exception {
        when(areaRepository.existsByName("Indoor")).thenReturn(true);

        mockMvc.perform(post("/api/admin/areas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \" Indoor \"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("AREA_NAME_ALREADY_EXISTS"));
        verify(areaRepository, never()).saveAndFlush(any(Area.class));
    }

    @Test
    void adminCanUpdateAreaNameAndDescription() throws Exception {
        Area area = area(1L, "Indoor", "Original", true);
        when(areaRepository.findById(1L)).thenReturn(Optional.of(area));
        when(areaRepository.saveAndFlush(area)).thenReturn(area);

        mockMvc.perform(patch("/api/admin/areas/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": " Main Indoor ", "description": " Main dining room "}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Main Indoor"))
                .andExpect(jsonPath("$.description").value("Main dining room"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void updateRetainingOwnNameSucceedsAndCannotChangeIdOrActive() throws Exception {
        Area area = area(1L, "Indoor", "Original", false);
        when(areaRepository.findById(1L)).thenReturn(Optional.of(area));
        when(areaRepository.saveAndFlush(area)).thenReturn(area);

        mockMvc.perform(patch("/api/admin/areas/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": " Indoor ", "id": 999, "active": true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Indoor"))
                .andExpect(jsonPath("$.description").value("Original"))
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void updateToAnotherAreasNameReturnsConflict() throws Exception {
        when(areaRepository.findById(1L)).thenReturn(Optional.of(area(1L, "Indoor", null, true)));
        when(areaRepository.existsByNameAndIdNot("Garden", 1L)).thenReturn(true);

        mockMvc.perform(patch("/api/admin/areas/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Garden\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("AREA_NAME_ALREADY_EXISTS"));
        verify(areaRepository, never()).saveAndFlush(any(Area.class));
    }

    @Test
    void updateRejectsBlankSuppliedName() throws Exception {
        when(areaRepository.findById(1L)).thenReturn(Optional.of(area(1L, "Indoor", null, true)));

        mockMvc.perform(patch("/api/admin/areas/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_AREA_NAME"));
        verify(areaRepository, never()).saveAndFlush(any(Area.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"id\": 999, \"active\": false}"})
    void emptyOrNonEditablePatchReturnsBadRequest(String body) throws Exception {
        mockMvc.perform(patch("/api/admin/areas/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EMPTY_PATCH"));
        verifyNoInteractions(areaRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"PATCH", "DEACTIVATE"})
    void unknownAreaIdReturnsNotFound(String operation) throws Exception {
        mockMvc.perform(request(operation))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("AREA_NOT_FOUND"));
        verify(areaRepository, never()).saveAndFlush(any(Area.class));
    }

    @Test
    void adminCanDeactivateIdempotentlyAndListTheRetainedArea() throws Exception {
        Area area = area(1L, "Indoor", "Main dining room", true);
        when(areaRepository.findById(1L)).thenReturn(Optional.of(area));
        when(areaRepository.saveAndFlush(area)).thenReturn(area);
        when(areaRepository.findAllByOrderByNameAsc()).thenReturn(List.of(area));

        for (int i = 0; i < 2; i++) {
            mockMvc.perform(patch("/api/admin/areas/1/deactivate"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.active").value(false));
        }
        mockMvc.perform(get("/api/admin/areas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Indoor"))
                .andExpect(jsonPath("$[0].active").value(false));
        verify(areaRepository, times(2)).findById(1L);
        verify(areaRepository, times(2)).saveAndFlush(area);
        verify(areaRepository).findAllByOrderByNameAsc();
        verifyNoMoreInteractions(areaRepository);
    }

    @Test
    void hardDeleteEndpointIsNotSupported() throws Exception {
        mockMvc.perform(delete("/api/admin/areas/1"))
                .andExpect(status().isMethodNotAllowed());
        verifyNoInteractions(areaRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "POST", "PATCH", "DEACTIVATE"})
    @WithAnonymousUser
    void anonymousCannotAccessAnyAreaEndpoint(String operation) throws Exception {
        mockMvc.perform(request(operation))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        verifyNoInteractions(areaRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "POST", "PATCH", "DEACTIVATE"})
    @WithMockUser(roles = "CUSTOMER")
    void customerCannotAccessAnyAreaEndpoint(String operation) throws Exception {
        mockMvc.perform(request(operation))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        verifyNoInteractions(areaRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "POST", "PATCH", "DEACTIVATE"})
    @WithMockUser(roles = "STAFF")
    void staffCannotAccessAnyAreaEndpoint(String operation) throws Exception {
        mockMvc.perform(request(operation))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        verifyNoInteractions(areaRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "CUSTOMER", "STAFF"})
    @WithAnonymousUser
    void bearerAuthenticationUsesExistingJwtFilterAndRoleRules(String role) throws Exception {
        AccountRole accountRole = AccountRole.valueOf(role);
        Account account = new Account();
        account.setId(1L);
        account.setRole(accountRole);
        when(jwtService.parseAndValidate("test-token")).thenReturn(new JwtService.JwtClaims(1L, accountRole));
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        if (accountRole == AccountRole.ADMIN) {
            when(areaRepository.findAllByOrderByNameAsc()).thenReturn(List.of());
        }

        mockMvc.perform(get("/api/admin/areas").header("Authorization", "Bearer test-token"))
                .andExpect(accountRole == AccountRole.ADMIN ? status().isOk() : status().isForbidden());
        if (accountRole != AccountRole.ADMIN) {
            verifyNoInteractions(areaRepository);
        }
    }

    private MockHttpServletRequestBuilder request(String operation) {
        return switch (operation) {
            case "GET" -> get("/api/admin/areas");
            case "POST" -> post("/api/admin/areas").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\": \"Indoor\"}");
            case "PATCH" -> patch("/api/admin/areas/1").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\": \"Indoor\"}");
            case "DEACTIVATE" -> patch("/api/admin/areas/1/deactivate");
            default -> throw new IllegalArgumentException("Unknown operation: " + operation);
        };
    }

    private Area area(Long id, String name, String description, boolean active) {
        Area area = new Area();
        area.setId(id);
        area.setName(name);
        area.setDescription(description);
        area.setActive(active);
        return area;
    }
}
