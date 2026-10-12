package com.sagares.saga_res_api.table.controller;

import com.sagares.saga_res_api.table.dto.admin.AdminRestaurantTableResponse;
import com.sagares.saga_res_api.table.dto.admin.CreateRestaurantTableRequest;
import com.sagares.saga_res_api.table.dto.admin.UpdateRestaurantTableRequest;
import com.sagares.saga_res_api.table.service.AdminRestaurantTableService;
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
@RequestMapping("/api/admin/tables")
@RequiredArgsConstructor
public class AdminRestaurantTableController {

    private final AdminRestaurantTableService adminRestaurantTableService;

    @GetMapping
    public List<AdminRestaurantTableResponse> getTables() {
        return adminRestaurantTableService.listTables();
    }

    @PostMapping
    public ResponseEntity<AdminRestaurantTableResponse> createTable(
            @Valid @RequestBody CreateRestaurantTableRequest request
    ) {
        AdminRestaurantTableResponse response = adminRestaurantTableService.createTable(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}")
    public AdminRestaurantTableResponse updateTable(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRestaurantTableRequest request
    ) {
        return adminRestaurantTableService.updateTable(id, request);
    }

    @PatchMapping("/{id}/deactivate")
    public AdminRestaurantTableResponse deactivateTable(@PathVariable Long id) {
        return adminRestaurantTableService.deactivateTable(id);
    }
}
