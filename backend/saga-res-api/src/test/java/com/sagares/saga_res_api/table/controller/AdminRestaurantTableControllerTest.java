package com.sagares.saga_res_api.table.controller;

import com.sagares.saga_res_api.account.repository.AccountRepository;
import com.sagares.saga_res_api.common.exception.GlobalExceptionHandler;
import com.sagares.saga_res_api.security.JwtAuthenticationFilter;
import com.sagares.saga_res_api.security.JwtService;
import com.sagares.saga_res_api.security.RestAccessDeniedHandler;
import com.sagares.saga_res_api.security.RestAuthenticationEntryPoint;
import com.sagares.saga_res_api.security.SecurityConfig;
import com.sagares.saga_res_api.table.entity.Area;
import com.sagares.saga_res_api.table.entity.RestaurantTable;
import com.sagares.saga_res_api.table.repository.AreaRepository;
import com.sagares.saga_res_api.table.repository.RestaurantTableRepository;
import com.sagares.saga_res_api.table.service.AdminRestaurantTableService;
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

@WebMvcTest(AdminRestaurantTableController.class)
@AutoConfigureMockMvc
@WithMockUser(roles = "ADMIN")
@Import({
        SecurityConfig.class,
        GlobalExceptionHandler.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class,
        AdminRestaurantTableService.class,
        AdminRestaurantTableControllerTest.JwtFilterTestConfig.class
})
class AdminRestaurantTableControllerTest {

    private static final String CREATE_BODY = """
            {"code": " T01 ", "areaId": 1, "minCapacity": 1, "maxCapacity": 2}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RestaurantTableRepository restaurantTableRepository;

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
    void adminCanListActiveAndInactiveTablesWithAreaDetails() throws Exception {
        RestaurantTable garden = table(false);
        garden.setArea(area(2L, "Garden", false));
        RestaurantTable indoor = table(true);
        indoor.setId(2L);
        indoor.setCode("T02");
        when(restaurantTableRepository.findAllByOrderByArea_NameAscCodeAsc()).thenReturn(List.of(garden, indoor));

        mockMvc.perform(get("/api/admin/tables"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].code").value("T01"))
                .andExpect(jsonPath("$[0].area.id").value(2))
                .andExpect(jsonPath("$[0].area.name").value("Garden"))
                .andExpect(jsonPath("$[0].minCapacity").value(1))
                .andExpect(jsonPath("$[0].maxCapacity").value(2))
                .andExpect(jsonPath("$[0].active").value(false))
                .andExpect(jsonPath("$[1].area.name").value("Indoor"))
                .andExpect(jsonPath("$[1].active").value(true));
    }

    @Test
    void adminCanCreateTableWithActiveDefaultAndCannotSupplyIdOrActive() throws Exception {
        when(areaRepository.findById(1L)).thenReturn(Optional.of(area(1L, "Indoor", true)));
        when(restaurantTableRepository.saveAndFlush(any(RestaurantTable.class))).thenAnswer(invocation -> {
            RestaurantTable saved = invocation.getArgument(0);
            assertThat(saved.getId()).isNull();
            saved.setId(1L);
            return saved;
        });

        mockMvc.perform(post("/api/admin/tables").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id": 999, "active": false, "code": " T01 ",
                                 "areaId": 1, "minCapacity": 1, "maxCapacity": 2}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.code").value("T01"))
                .andExpect(jsonPath("$.area.id").value(1))
                .andExpect(jsonPath("$.area.name").value("Indoor"))
                .andExpect(jsonPath("$.minCapacity").value(1))
                .andExpect(jsonPath("$.maxCapacity").value(2))
                .andExpect(jsonPath("$.active").value(true));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}",
            "{\"code\":\"   \",\"areaId\":1,\"minCapacity\":1,\"maxCapacity\":2}",
            "{\"code\":\"123456789012345678901\",\"areaId\":1,\"minCapacity\":1,\"maxCapacity\":2}",
            "{\"code\":\"T01\",\"minCapacity\":1,\"maxCapacity\":2}",
            "{\"code\":\"T01\",\"areaId\":1,\"maxCapacity\":2}",
            "{\"code\":\"T01\",\"areaId\":1,\"minCapacity\":1}",
            "{\"code\":\"T01\",\"areaId\":1,\"minCapacity\":0,\"maxCapacity\":2}",
            "{\"code\":\"T01\",\"areaId\":1,\"minCapacity\":-1,\"maxCapacity\":2}"
    })
    void invalidCreateInputIsRejectedAtApiBoundary(String body) throws Exception {
        mockMvc.perform(post("/api/admin/tables").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        verifyNoInteractions(restaurantTableRepository, areaRepository);
    }

    @Test
    void duplicateCodeReturnsConflict() throws Exception {
        when(restaurantTableRepository.existsByCode("T01")).thenReturn(true);

        mockMvc.perform(post("/api/admin/tables").contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TABLE_CODE_ALREADY_EXISTS"));
        verify(restaurantTableRepository, never()).saveAndFlush(any());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void createRejectsUnknownOrInactiveArea(boolean missing) throws Exception {
        when(areaRepository.findById(1L)).thenReturn(missing ? Optional.empty() : Optional.of(area(1L, "Indoor", false)));

        mockMvc.perform(post("/api/admin/tables").contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(missing ? status().isNotFound() : status().isConflict())
                .andExpect(jsonPath("$.code").value(missing ? "AREA_NOT_FOUND" : "AREA_INACTIVE"));
        verify(restaurantTableRepository, never()).saveAndFlush(any());
    }

    @Test
    void createRejectsMaximumCapacityBelowMinimum() throws Exception {
        when(areaRepository.findById(1L)).thenReturn(Optional.of(area(1L, "Indoor", true)));

        mockMvc.perform(post("/api/admin/tables").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"T01\",\"areaId\":1,\"minCapacity\":3,\"maxCapacity\":2}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TABLE_CAPACITY"));
        verify(restaurantTableRepository, never()).saveAndFlush(any());
    }

    @Test
    void createAcceptsEqualMinimumAndMaximumCapacity() throws Exception {
        when(areaRepository.findById(1L)).thenReturn(Optional.of(area(1L, "Indoor", true)));
        when(restaurantTableRepository.saveAndFlush(any(RestaurantTable.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/admin/tables").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"T01\",\"areaId\":1,\"minCapacity\":2,\"maxCapacity\":2}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.minCapacity").value(2))
                .andExpect(jsonPath("$.maxCapacity").value(2));
    }

    @Test
    void adminCanUpdateCodeActiveAreaAndCapacity() throws Exception {
        RestaurantTable table = table(true);
        when(restaurantTableRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(table));
        when(areaRepository.findById(2L)).thenReturn(Optional.of(area(2L, "Garden", true)));
        when(restaurantTableRepository.saveAndFlush(table)).thenReturn(table);

        mockMvc.perform(patch("/api/admin/tables/1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\" T02 \",\"areaId\":2,\"minCapacity\":2,\"maxCapacity\":4}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.code").value("T02"))
                .andExpect(jsonPath("$.area.id").value(2))
                .andExpect(jsonPath("$.area.name").value("Garden"))
                .andExpect(jsonPath("$.minCapacity").value(2))
                .andExpect(jsonPath("$.maxCapacity").value(4))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void updateKeepingOwnCodeSucceedsAndCannotChangeIdOrActive() throws Exception {
        RestaurantTable table = table(false);
        when(restaurantTableRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(table));
        when(restaurantTableRepository.saveAndFlush(table)).thenReturn(table);

        mockMvc.perform(patch("/api/admin/tables/1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\" T01 \",\"id\":999,\"active\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.code").value("T01"))
                .andExpect(jsonPath("$.active").value(false));
        verify(restaurantTableRepository, never()).existsByCodeAndIdNot(any(), any());
    }

    @Test
    void updateRejectsAnotherTablesCode() throws Exception {
        when(restaurantTableRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(table(true)));
        when(restaurantTableRepository.existsByCodeAndIdNot("T02", 1L)).thenReturn(true);

        mockMvc.perform(patch("/api/admin/tables/1").contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"T02\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TABLE_CODE_ALREADY_EXISTS"));
        verify(restaurantTableRepository, never()).saveAndFlush(any());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void updateRejectsUnknownOrInactiveTargetArea(boolean missing) throws Exception {
        when(restaurantTableRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(table(true)));
        when(areaRepository.findById(2L)).thenReturn(missing ? Optional.empty() : Optional.of(area(2L, "Garden", false)));

        mockMvc.perform(patch("/api/admin/tables/1").contentType(MediaType.APPLICATION_JSON).content("{\"areaId\":2}"))
                .andExpect(missing ? status().isNotFound() : status().isConflict())
                .andExpect(jsonPath("$.code").value(missing ? "AREA_NOT_FOUND" : "AREA_INACTIVE"));
        verify(restaurantTableRepository, never()).saveAndFlush(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"minCapacity\":3}", "{\"code\":\"   \"}"})
    void updateRejectsInvalidMergedCapacityOrBlankCode(String body) throws Exception {
        when(restaurantTableRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(table(true)));

        mockMvc.perform(patch("/api/admin/tables/1").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verify(restaurantTableRepository, never()).saveAndFlush(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"minCapacity\":0}", "{\"maxCapacity\":-1}", "{\"code\":\"123456789012345678901\"}"})
    void invalidUpdateInputIsRejectedAtApiBoundary(String body) throws Exception {
        mockMvc.perform(patch("/api/admin/tables/1").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        verifyNoInteractions(restaurantTableRepository, areaRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"id\":999,\"active\":false}", "{\"area\":{\"id\":2}}"})
    void emptyOrNonEditablePatchIsRejected(String body) throws Exception {
        mockMvc.perform(patch("/api/admin/tables/1").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EMPTY_PATCH"));
        verifyNoInteractions(restaurantTableRepository, areaRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"PATCH", "DEACTIVATE"})
    void unknownTableReturnsNotFound(String operation) throws Exception {
        mockMvc.perform(request(operation))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TABLE_NOT_FOUND"));
    }

    @Test
    void adminCanDeactivateRepeatedlyAndListTheRetainedRow() throws Exception {
        RestaurantTable table = table(true);
        when(restaurantTableRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(table));
        when(restaurantTableRepository.saveAndFlush(table)).thenReturn(table);
        when(restaurantTableRepository.findAllByOrderByArea_NameAscCodeAsc()).thenReturn(List.of(table));

        for (int i = 0; i < 2; i++) {
            mockMvc.perform(patch("/api/admin/tables/1/deactivate"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.active").value(false));
        }
        mockMvc.perform(get("/api/admin/tables"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].active").value(false));
        verify(restaurantTableRepository, times(2)).findByIdForUpdate(1L);
        verify(restaurantTableRepository, times(2)).saveAndFlush(table);
        verify(restaurantTableRepository).findAllByOrderByArea_NameAscCodeAsc();
        verifyNoMoreInteractions(restaurantTableRepository);
    }

    @Test
    void deleteEndpointIsNotSupported() throws Exception {
        mockMvc.perform(delete("/api/admin/tables/1")).andExpect(status().isMethodNotAllowed());
        verifyNoInteractions(restaurantTableRepository, areaRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "POST", "PATCH", "DEACTIVATE"})
    @WithMockUser(roles = "CUSTOMER")
    void customerIsForbiddenOnAllTableEndpoints(String operation) throws Exception {
        mockMvc.perform(request(operation)).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        verifyNoInteractions(restaurantTableRepository, areaRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "POST", "PATCH", "DEACTIVATE"})
    @WithMockUser(roles = "STAFF")
    void staffIsForbiddenOnAllTableEndpoints(String operation) throws Exception {
        mockMvc.perform(request(operation)).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        verifyNoInteractions(restaurantTableRepository, areaRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "POST", "PATCH", "DEACTIVATE"})
    @WithAnonymousUser
    void anonymousIsUnauthorizedOnAllTableEndpoints(String operation) throws Exception {
        mockMvc.perform(request(operation)).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        verifyNoInteractions(restaurantTableRepository, areaRepository);
    }

    private MockHttpServletRequestBuilder request(String operation) {
        return switch (operation) {
            case "GET" -> get("/api/admin/tables");
            case "POST" -> post("/api/admin/tables").contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY);
            case "PATCH" -> patch("/api/admin/tables/1").contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"T02\"}");
            case "DEACTIVATE" -> patch("/api/admin/tables/1/deactivate");
            default -> throw new IllegalArgumentException("Unknown operation: " + operation);
        };
    }

    private Area area(Long id, String name, boolean active) {
        Area area = new Area();
        area.setId(id);
        area.setName(name);
        area.setActive(active);
        return area;
    }

    private RestaurantTable table(boolean active) {
        RestaurantTable table = new RestaurantTable();
        table.setId(1L);
        table.setCode("T01");
        table.setArea(area(1L, "Indoor", true));
        table.setMinCapacity((short) 1);
        table.setMaxCapacity((short) 2);
        table.setActive(active);
        return table;
    }
}
