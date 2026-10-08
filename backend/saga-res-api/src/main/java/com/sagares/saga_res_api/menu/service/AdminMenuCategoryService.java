package com.sagares.saga_res_api.menu.service;

import com.sagares.saga_res_api.common.exception.BusinessException;
import com.sagares.saga_res_api.menu.dto.admin.AdminCategoryResponse;
import com.sagares.saga_res_api.menu.dto.admin.CreateCategoryRequest;
import com.sagares.saga_res_api.menu.dto.admin.UpdateCategoryRequest;
import com.sagares.saga_res_api.menu.entity.Category;
import com.sagares.saga_res_api.menu.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminMenuCategoryService {

    private static final int CATEGORY_NAME_MAX_LENGTH = 100;

    private final CategoryRepository categoryRepository;

    public List<AdminCategoryResponse> listCategories() {
        return categoryRepository.findAllByOrderByDisplayOrderAscNameAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AdminCategoryResponse createCategory(CreateCategoryRequest request) {
        String name = normalizeName(request.name());
        if (categoryRepository.existsByName(name)) {
            throw duplicateCategoryName();
        }

        Category category = new Category();
        category.setName(name);
        category.setDisplayOrder(request.displayOrder());
        category.setActive(true);

        return toResponse(save(category));
    }

    @Transactional
    public AdminCategoryResponse updateCategory(Long id, UpdateCategoryRequest request) {
        if (request.name() == null && request.displayOrder() == null && request.active() == null) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "EMPTY_PATCH",
                    "At least one category field must be supplied."
            );
        }

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "CATEGORY_NOT_FOUND",
                        "Category not found."
                ));

        if (request.name() != null) {
            String name = normalizeName(request.name());
            if (!name.equals(category.getName()) && categoryRepository.existsByNameAndIdNot(name, id)) {
                throw duplicateCategoryName();
            }
            category.setName(name);
        }

        if (request.displayOrder() != null) {
            category.setDisplayOrder(request.displayOrder());
        }

        if (request.active() != null) {
            category.setActive(request.active());
        }

        return toResponse(save(category));
    }

    private Category save(Category category) {
        try {
            return categoryRepository.saveAndFlush(category);
        } catch (DataIntegrityViolationException exception) {
            throw duplicateCategoryName();
        }
    }

    private String normalizeName(String name) {
        String normalized = name == null ? "" : name.trim();
        if (normalized.isBlank()) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_CATEGORY_NAME",
                    "Tên loại không được để trống"
            );
        }
        if (normalized.length() > CATEGORY_NAME_MAX_LENGTH) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_CATEGORY_NAME",
                    "Category name must be at most 100 characters."
            );
        }
        return normalized;
    }

    private BusinessException duplicateCategoryName() {
        return new BusinessException(
                HttpStatus.CONFLICT,
                "DUPLICATE_CATEGORY_NAME",
                "Category name already exists."
        );
    }

    private AdminCategoryResponse toResponse(Category category) {
        return new AdminCategoryResponse(
                category.getId(),
                category.getName(),
                category.getDisplayOrder(),
                category.isActive()
        );
    }
}
