package com.sagares.saga_res_api.menu.controller;

import com.sagares.saga_res_api.common.dto.PageResponse;
import com.sagares.saga_res_api.menu.dto.publicmenu.DishDetailResponse;
import com.sagares.saga_res_api.menu.dto.publicmenu.DishSummaryResponse;
import com.sagares.saga_res_api.menu.enums.PriceRange;
import com.sagares.saga_res_api.menu.service.PublicMenuDishService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/menu/dishes")
@RequiredArgsConstructor
@Tag(name = "Menu", description = "Public menu browsing")
public class PublicMenuDishController {

    private final PublicMenuDishService service;

    @GetMapping
    @Operation(summary = "Browse, search and filter public dishes",
            description = "Hidden dishes are excluded. Sold out dishes remain visible; category active state does not affect visibility.")
    public PageResponse<DishSummaryResponse> listDishes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long tagId,
            @RequestParam(required = false)PriceRange priceRange
            ) {
        return service.listDishes(page, size, search, categoryId, tagId, priceRange);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get public dish detail",
            description = "Hidden and nonexistent dishes return the same not found response.")
    public DishDetailResponse getDish(@PathVariable Long id) {
        return service.getDish(id);
    }
}
