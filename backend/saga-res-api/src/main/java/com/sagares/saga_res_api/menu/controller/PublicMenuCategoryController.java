package com.sagares.saga_res_api.menu.controller;

import com.sagares.saga_res_api.menu.dto.publicmenu.PublicDishCategoryResponse;
import com.sagares.saga_res_api.menu.service.PublicMenuCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/menu/categories")
@RequiredArgsConstructor
@Tag(name = "Menu", description = "Public menu browsing")
public class PublicMenuCategoryController {

    private final PublicMenuCategoryService service;

    @GetMapping
    @Operation(summary = "List active public menu categories",
            description = "Returns active categories ordered by displayOrder, then name, ascending. Returns an empty array when none exist.")
    public List<PublicDishCategoryResponse> listCategories() {
        return service.listCategories();
    }
}
