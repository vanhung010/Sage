package com.sagares.saga_res_api.restaurant.controller;

import com.sagares.saga_res_api.account.entity.Account;
import com.sagares.saga_res_api.account.entity.AccountRole;
import com.sagares.saga_res_api.account.repository.AccountRepository;
import com.sagares.saga_res_api.common.exception.GlobalExceptionHandler;
import com.sagares.saga_res_api.restaurant.entity.OpeningHour;
import com.sagares.saga_res_api.restaurant.repository.OpeningHourRepository;
import com.sagares.saga_res_api.reservation.repository.ReservationRepository;
import com.sagares.saga_res_api.restaurant.service.OpeningHourService;
import com.sagares.saga_res_api.security.JwtAuthenticationFilter;
import com.sagares.saga_res_api.security.JwtService;
import com.sagares.saga_res_api.security.RestAccessDeniedHandler;
import com.sagares.saga_res_api.security.RestAuthenticationEntryPoint;
import com.sagares.saga_res_api.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.contains;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({OpeningHourController.class, AdminOpeningHourController.class})
@Import({SecurityConfig.class, GlobalExceptionHandler.class, RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class, JwtAuthenticationFilter.class, OpeningHourService.class})
class OpeningHourControllerTest {

    private static final LocalTime OPEN = LocalTime.of(10, 0);
    private static final LocalTime CLOSE = LocalTime.of(22, 0);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OpeningHourRepository repository;

    @MockitoBean
    private ReservationRepository reservationRepository;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private AccountRepository accountRepository;

    @Test
    void guestCanReadCalendarOrderedPublicHoursWithoutAdminFields() throws Exception {
        when(repository.findAll()).thenReturn(List.of(hour(0), hour(6), hour(5), hour(4), hour(3), hour(2), hour(1)));

        mockMvc.perform(get("/api/opening-hours"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(7)))
                .andExpect(jsonPath("$[*].dayOfWeek").value(contains(
                        "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY")))
                .andExpect(jsonPath("$[0].openTime").value("10:00:00"))
                .andExpect(jsonPath("$[0].closeTime").value("22:00:00"))
                .andExpect(jsonPath("$[0].closed").value(false))
                .andExpect(jsonPath("$[0].*", hasSize(4)))
                .andExpect(jsonPath("$[*].id").isEmpty());
        verifyNoInteractions(jwtService, accountRepository);
    }

    @ParameterizedTest
    @EnumSource(AccountRole.class)
    void everyAuthenticatedRoleCanReadPublicHoursUsingBearerToken(AccountRole role) throws Exception {
        Account account = new Account();
        account.setId(42L);
        account.setRole(role);
        when(jwtService.parseAndValidate("test-token")).thenReturn(new JwtService.JwtClaims(42L, role));
        when(accountRepository.findById(42L)).thenReturn(Optional.of(account));
        when(repository.findAll()).thenReturn(List.of(hour(1)));

        mockMvc.perform(get("/api/opening-hours").header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].dayOfWeek").value("MONDAY"));
    }

    @Test
    void closedDayIsVisibleAsClosedEvenWhenLegacyTimesAreNull() throws Exception {
        OpeningHour hour = hour(0);
        hour.setClosed(true);
        hour.setOpenTime(null);
        hour.setCloseTime(null);
        when(repository.findAll()).thenReturn(List.of(hour));

        mockMvc.perform(get("/api/opening-hours"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].dayOfWeek").value("SUNDAY"))
                .andExpect(jsonPath("$[0].closed").value(true))
                .andExpect(jsonPath("$[0].openTime").isEmpty())
                .andExpect(jsonPath("$[0].closeTime").isEmpty());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminListIncludesExistingWeekdayPrimaryKey() throws Exception {
        when(repository.findAll()).thenReturn(List.of(hour(0), hour(2), hour(1)));
        mockMvc.perform(get("/api/admin/opening-hours"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id").value(contains(1, 2, 0)))
                .andExpect(jsonPath("$[0].dayOfWeek").value("MONDAY"));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2})
    @WithMockUser(roles = "ADMIN")
    void adminCreatesMondayAndTuesday(int id) throws Exception {
        String day = id == 1 ? "MONDAY" : "TUESDAY";
        when(repository.insertIfAbsent((short) id, OPEN, CLOSE)).thenReturn(1);
        mockMvc.perform(post("/api/admin/opening-hours").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dayOfWeek":"%s","openTime":"10:00","closeTime":"22:00"}
                                """.formatted(day)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.dayOfWeek").value(day))
                .andExpect(jsonPath("$.openTime").value("10:00:00"))
                .andExpect(jsonPath("$.closeTime").value("22:00:00"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"openTime\":\"10:00\",\"closeTime\":\"22:00\"}",
            "{\"dayOfWeek\":\"MONDAY\",\"closeTime\":\"22:00\"}",
            "{\"dayOfWeek\":\"MONDAY\",\"openTime\":\"10:00\"}",
            "{\"dayOfWeek\":null,\"openTime\":\"10:00\",\"closeTime\":\"22:00\"}",
            "{\"dayOfWeek\":\"MONDAY\",\"openTime\":null,\"closeTime\":\"22:00\"}",
            "{\"dayOfWeek\":\"MONDAY\",\"openTime\":\"10:00\",\"closeTime\":null}"
    })
    @WithMockUser(roles = "ADMIN")
    void createRequiresEachField(String body) throws Exception {
        mockMvc.perform(post("/api/admin/opening-hours").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        verifyNoInteractions(repository);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"dayOfWeek\":\"FUNDAY\",\"openTime\":\"10:00\",\"closeTime\":\"22:00\"}",
            "{\"dayOfWeek\":1,\"openTime\":\"10:00\",\"closeTime\":\"22:00\"}",
            "{\"dayOfWeek\":\"MONDAY\",\"openTime\":\"25:00\",\"closeTime\":\"22:00\"}",
            "{\"dayOfWeek\":\"MONDAY\",\"openTime\":\"10:00Z\",\"closeTime\":\"22:00\"}",
            "{invalid-json}"
    })
    @WithMockUser(roles = "ADMIN")
    void malformedCreateReturnsSafeVietnameseError(String body) throws Exception {
        mockMvc.perform(post("/api/admin/opening-hours").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("Dữ liệu yêu cầu không hợp lệ"))
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
        verifyNoInteractions(repository);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void duplicateReturnsConflict() throws Exception {
        mockMvc.perform(post("/api/admin/opening-hours").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dayOfWeek\":\"MONDAY\",\"openTime\":\"10:00\",\"closeTime\":\"22:00\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("OPENING_HOUR_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.message").value("Giờ mở cửa của ngày này đã được cấu hình"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"10:00", "23:00"})
    @WithMockUser(roles = "ADMIN")
    void createRejectsEqualAndReversedTimes(String openTime) throws Exception {
        mockMvc.perform(post("/api/admin/opening-hours").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dayOfWeek":"MONDAY","openTime":"%s","closeTime":"10:00"}
                                """.formatted(openTime)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Giờ đóng cửa phải sau giờ mở cửa"));
        verifyNoInteractions(repository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"openTime\":\"09:30\"}", "{\"closeTime\":\"22:30\"}",
            "{\"dayOfWeek\":\"MONDAY\",\"openTime\":\"09:30\",\"closeTime\":\"22:30\"}"})
    @WithMockUser(roles = "ADMIN")
    void adminCanPatchTimesAndPublicReadSeesTheChange(String body) throws Exception {
        OpeningHour hour = hour(1);
        when(repository.findByIdForUpdate((short) 1)).thenReturn(Optional.of(hour));
        when(repository.saveAndFlush(hour)).thenReturn(hour);
        when(repository.findAll()).thenReturn(List.of(hour));
        String openTime = body.contains("09:30") ? "09:30:00" : "10:00:00";
        String closeTime = body.contains("22:30") ? "22:30:00" : "22:00:00";

        mockMvc.perform(patch("/api/admin/opening-hours/1").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.openTime").value(openTime))
                .andExpect(jsonPath("$.closeTime").value(closeTime));
        mockMvc.perform(get("/api/opening-hours"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].openTime").value(openTime))
                .andExpect(jsonPath("$[0].closeTime").value(closeTime));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"openTime\":\"22:00\"}", "{\"closeTime\":\"09:00\"}"})
    @WithMockUser(roles = "ADMIN")
    void patchValidatesMergedTimeInterval(String body) throws Exception {
        when(repository.findByIdForUpdate((short) 1)).thenReturn(Optional.of(hour(1)));
        mockMvc.perform(patch("/api/admin/opening-hours/1").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Giờ đóng cửa phải sau giờ mở cửa"));
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void patchMissingRecordReturnsNotFound() throws Exception {
        mockMvc.perform(patch("/api/admin/opening-hours/7").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"openTime\":\"09:30\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Cấu hình giờ mở cửa không tồn tại"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void patchRejectsChangedWeekdayInsteadOfChangingPrimaryKey() throws Exception {
        when(repository.findByIdForUpdate((short) 1)).thenReturn(Optional.of(hour(1)));
        mockMvc.perform(patch("/api/admin/opening-hours/1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dayOfWeek\":\"TUESDAY\",\"openTime\":\"09:30\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("OPENING_HOUR_DAY_IMMUTABLE"));
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void noDeleteEndpointIsExposed() throws Exception {
        mockMvc.perform(delete("/api/admin/opening-hours/1")).andExpect(status().isMethodNotAllowed());
        verifyNoInteractions(repository);
    }

    private OpeningHour hour(int id) {
        OpeningHour hour = new OpeningHour();
        hour.setDayOfWeek((short) id);
        hour.setOpenTime(OPEN);
        hour.setCloseTime(CLOSE);
        return hour;
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void patchReturnsImpactedReservationIdsOnlyToAdmin() throws Exception {
        OpeningHour hour = hour(1);
        when(repository.findByIdForUpdate((short) 1)).thenReturn(Optional.of(hour));
        when(repository.saveAndFlush(hour)).thenReturn(hour);
        when(reservationRepository.findIdsOutsideOpeningHours((short) 1, OPEN, LocalTime.of(21, 0), false))
                .thenReturn(List.of(12L, 34L));
        mockMvc.perform(patch("/api/admin/opening-hours/1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"closeTime\":\"21:00\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.impactedReservationIds").value(contains(12, 34)));
    }
}
