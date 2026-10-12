package com.sagares.saga_res_api.table.controller;

import com.sagares.saga_res_api.table.dto.admin.AdminAreaResponse;
import com.sagares.saga_res_api.table.dto.admin.CreateAreaRequest;
import com.sagares.saga_res_api.table.dto.admin.UpdateAreaRequest;
import com.sagares.saga_res_api.table.service.AdminAreaService;
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
@RequestMapping("/api/admin/areas")
@RequiredArgsConstructor
public class AdminAreaController {

    private final AdminAreaService adminAreaService;

    @GetMapping
    public List<AdminAreaResponse> getAreas() {
        return adminAreaService.listAreas();
    }

    @PostMapping
    public ResponseEntity<AdminAreaResponse> createArea(
            @Valid @RequestBody CreateAreaRequest request) {
        AdminAreaResponse response = adminAreaService.createArea(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PatchMapping("/{id}")
    public AdminAreaResponse updateArea(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAreaRequest request
    ) {
        return adminAreaService.updateArea(id, request);
    }

    @PatchMapping("/{id}/deactivate")
    public AdminAreaResponse deactivateArea(@PathVariable Long id) {
        return adminAreaService.deactivateArea(id);
    }
}
