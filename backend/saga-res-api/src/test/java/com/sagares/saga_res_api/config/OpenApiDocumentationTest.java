package com.sagares.saga_res_api.config;

import com.sagares.saga_res_api.auth.controller.AuthController;
import com.sagares.saga_res_api.auth.service.AuthService;
import com.sagares.saga_res_api.menu.controller.AdminMenuCategoryController;
import com.sagares.saga_res_api.menu.controller.AdminMenuTagController;
import com.sagares.saga_res_api.menu.service.AdminMenuCategoryService;
import com.sagares.saga_res_api.menu.service.AdminMenuTagService;
import com.sagares.saga_res_api.restaurant.controller.AdminOpeningHourController;
import com.sagares.saga_res_api.restaurant.controller.OpeningHourController;
import com.sagares.saga_res_api.restaurant.service.OpeningHourService;
import com.sagares.saga_res_api.table.controller.AdminAreaController;
import com.sagares.saga_res_api.table.controller.AdminRestaurantTableController;
import com.sagares.saga_res_api.table.service.AdminAreaService;
import com.sagares.saga_res_api.table.service.AdminRestaurantTableService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({
        AuthController.class,
        AdminMenuCategoryController.class,
        AdminMenuTagController.class,
        AdminOpeningHourController.class,
        OpeningHourController.class,
        AdminAreaController.class,
        AdminRestaurantTableController.class
})
@AutoConfigureMockMvc(addFilters = false)
@Import(OpenApiConfig.class)
class OpenApiDocumentationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private AdminMenuCategoryService adminMenuCategoryService;

    @MockitoBean
    private AdminMenuTagService adminMenuTagService;

    @MockitoBean
    private OpeningHourService openingHourService;

    @MockitoBean
    private AdminAreaService adminAreaService;

    @MockitoBean
    private AdminRestaurantTableService adminRestaurantTableService;

    @Test
    void generatedContractContainsOnlyImplementedEndpointsAndSharedSchemas() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths", aMapWithSize(22)))
                .andExpect(jsonPath("$['paths']['/api/auth/register']['post']['summary']")
                        .value("Register a customer account"))
                .andExpect(jsonPath("$['paths']['/api/auth/me']['get']['security'][0]['bearerAuth']").isArray())
                .andExpect(jsonPath("$['paths']['/api/admin/tables']['post']['requestBody']['required']").value(true))
                .andExpect(jsonPath("$['components']['securitySchemes']['bearerAuth']['scheme']").value("bearer"))
                .andExpect(jsonPath("$['components']['schemas']['ErrorResponse']['properties']", aMapWithSize(3)))
                .andExpect(jsonPath("$['components']['schemas']['AccountRole']['enum']", hasSize(3)));
    }
}
