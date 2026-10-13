package com.sagares.saga_res_api.menu.controller;

import com.sagares.saga_res_api.common.dto.PageResponse;
import com.sagares.saga_res_api.menu.dto.admin.AdminDishResponse;
import com.sagares.saga_res_api.menu.dto.admin.CreateDishRequest;
import com.sagares.saga_res_api.menu.dto.admin.UpdateDishRequest;
import com.sagares.saga_res_api.menu.dto.admin.UpdateDishStatusRequest;
import com.sagares.saga_res_api.menu.entity.DishStatus;
import com.sagares.saga_res_api.menu.service.AdminMenuDishService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/menu/dishes")
@RequiredArgsConstructor
@Tag(name = "Admin Menu Dishes", description = "Quản lý món ăn dành riêng cho quản trị viên.")
public class AdminMenuDishController {

    private final AdminMenuDishService adminMenuDishService;

    @GetMapping
    @Operation(summary = "Liệt kê món ăn cho quản trị viên")
    public PageResponse<AdminDishResponse> listDishes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) DishStatus status
    ) {
        return adminMenuDishService.listDishes(page, size, categoryId, status);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy chi tiết món ăn cho quản trị viên")
    public AdminDishResponse getDish(@PathVariable Long id) {
        return adminMenuDishService.getDish(id);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Tạo món ăn")
    public ResponseEntity<AdminDishResponse> createDish(
            @Valid @ModelAttribute CreateDishRequest request,
            @RequestPart(name = "image", required = false) MultipartFile image
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminMenuDishService.createDish(request, image));
    }

    @PatchMapping(path = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Cập nhật món ăn")
    public AdminDishResponse updateDish(
            @PathVariable Long id,
            @Valid @ModelAttribute UpdateDishRequest request,
            @RequestPart(name = "image", required = false) MultipartFile image
    ) {
        return adminMenuDishService.updateDish(id, request, image);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Thay đổi trạng thái món ăn")
    public AdminDishResponse updateDishStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDishStatusRequest request
    ) {
        return adminMenuDishService.updateDishStatus(id, request);
    }
}
