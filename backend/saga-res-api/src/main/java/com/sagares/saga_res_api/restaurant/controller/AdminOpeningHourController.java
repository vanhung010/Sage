package com.sagares.saga_res_api.restaurant.controller;

import com.sagares.saga_res_api.restaurant.dto.admin.AdminOpeningHourResponse;
import com.sagares.saga_res_api.restaurant.dto.admin.CreateOpeningHourRequest;
import com.sagares.saga_res_api.restaurant.dto.admin.UpdateOpeningHourRequest;
import com.sagares.saga_res_api.restaurant.service.OpeningHourService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/opening-hours")
@RequiredArgsConstructor
public class AdminOpeningHourController {

    private final OpeningHourService openingHourService;

    @GetMapping
    public List<AdminOpeningHourResponse> getOpeningHours() {
        return openingHourService.listAdminOpeningHours();
    }

    @PostMapping
    public ResponseEntity<AdminOpeningHourResponse> createOpeningHour(
            @Valid @RequestBody CreateOpeningHourRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(openingHourService.createOpeningHour(request));
    }

    @PatchMapping("/{id}")
    public AdminOpeningHourResponse updateOpeningHour(
            @PathVariable Short id, @Valid @RequestBody UpdateOpeningHourRequest request) {
        return openingHourService.updateOpeningHour(id, request);
    }
}
