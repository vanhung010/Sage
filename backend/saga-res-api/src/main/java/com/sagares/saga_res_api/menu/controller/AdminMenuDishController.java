package com.sagares.saga_res_api.menu.controller;

import com.sagares.saga_res_api.common.dto.PageResponse;
import com.sagares.saga_res_api.menu.dto.admin.*;
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
    @Operation(
            summary = "Lấy danh sách món ăn cho quản trị viên",
            description = """
                Phân trang và lọc theo danh mục, trạng thái.
                Không truyền categoryId hoặc status thì bỏ bộ lọc tương ứng.
                Mỗi món chỉ trả một nhãn đầu tiên theo name, sau đó id.
                Bao gồm món HIDDEN khi không lọc trạng thái.
                Quyền truy cập: ADMIN.
                """
    )
    public PageResponse<AdminDishListItemResponse> listDishes(
            @RequestParam(name = "page", defaultValue = "0")
            int page,

            @RequestParam(name = "size", defaultValue = "20")
            int size,

            @RequestParam(name = "categoryId", required = false)
            Long categoryId,

            @RequestParam(name = "status", required = false)
            DishStatus status
    ) {
        return adminMenuDishService.listDishes(
                page,
                size,
                categoryId,
                status
        );
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
