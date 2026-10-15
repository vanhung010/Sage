package com.sagares.saga_res_api.menu.dto.publicmenu;

import com.sagares.saga_res_api.menu.entity.DishStatus;
import java.util.List;

public record DishSummaryResponse(
        Long id,
        String name,
        String description,
        Integer price,
        String imageUrl,
        DishStatus status,
        PublicDishCategoryResponse category,
        List<PublicDishTagResponse> tags
) {}
