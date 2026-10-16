package com.sagares.saga_res_api.menu.service;

import com.sagares.saga_res_api.menu.dto.publicmenu.PublicDishCategoryResponse;
import com.sagares.saga_res_api.menu.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublicMenuCategoryService {

    private final CategoryRepository categoryRepository;

    public List<PublicDishCategoryResponse> listCategories() {
        return categoryRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc().stream()
                .map(category -> new PublicDishCategoryResponse(
                        category.getId(), category.getName(), category.getDisplayOrder()))
                .toList();
    }
}
