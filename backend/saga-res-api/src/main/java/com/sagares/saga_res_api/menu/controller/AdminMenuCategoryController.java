package com.sagares.saga_res_api.menu.controller;

import com.sagares.saga_res_api.menu.dto.admin.AdminCategoryResponse;
import com.sagares.saga_res_api.menu.dto.admin.CreateCategoryRequest;
import com.sagares.saga_res_api.menu.dto.admin.UpdateCategoryRequest;
import com.sagares.saga_res_api.menu.service.AdminMenuCategoryService;
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
@RequestMapping("/api/admin/menu/categories")
@RequiredArgsConstructor
public class AdminMenuCategoryController {

    private final AdminMenuCategoryService adminMenuCategoryService;

    @GetMapping
    public List<AdminCategoryResponse> getCategories() {

        return adminMenuCategoryService.listCategories();
    }

    @PostMapping
    public ResponseEntity<AdminCategoryResponse> createCategory(
            @Valid @RequestBody CreateCategoryRequest request
    ) {
        AdminCategoryResponse response = adminMenuCategoryService.createCategory(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PatchMapping("/{id}")
    public AdminCategoryResponse updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCategoryRequest request
    ) {
        return adminMenuCategoryService.updateCategory(id, request);
    }
}
