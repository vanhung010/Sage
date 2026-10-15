package com.sagares.saga_res_api.menu.service;

import com.sagares.saga_res_api.common.dto.PageResponse;
import com.sagares.saga_res_api.common.exception.BusinessException;
import com.sagares.saga_res_api.menu.dto.publicmenu.DishDetailResponse;
import com.sagares.saga_res_api.menu.dto.publicmenu.DishSummaryResponse;
import com.sagares.saga_res_api.menu.dto.publicmenu.PublicDishCategoryResponse;
import com.sagares.saga_res_api.menu.dto.publicmenu.PublicDishTagResponse;
import com.sagares.saga_res_api.menu.entity.Dish;
import com.sagares.saga_res_api.menu.entity.DishStatus;
import com.sagares.saga_res_api.menu.entity.Tag;
import com.sagares.saga_res_api.menu.enums.PriceRange;
import com.sagares.saga_res_api.menu.repository.DishRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublicMenuDishService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Sort PUBLIC_DISH_SORT = Sort.by(
            Sort.Order.asc("displayOrder"), Sort.Order.asc("name"), Sort.Order.asc("id"));
    private final DishRepository dishRepository;

    public PageResponse<DishSummaryResponse> listDishes(int page, int size, String search, Long categoryId, Long tagId, PriceRange priceRange) {

        Integer effectiveMinPrice = null;
        Integer effectiveMaxPrice = null;

        if (priceRange != null) {
            switch (priceRange) {
                case UNDER_100K -> effectiveMaxPrice = 99_999;
                case FROM_100K_TO_200K -> {
                    effectiveMinPrice = 100_000;
                    effectiveMaxPrice = 200_000;
                }
                case FROM_200K_TO_300K -> {
                    effectiveMinPrice = 200_000;
                    effectiveMaxPrice = 300_000;
                }
                case OVER_300K -> effectiveMinPrice = 300_001;
            }
        }

        validate(page, size, categoryId, tagId);
        String normalizedSearch = search == null ? null : search.trim();
        String pattern = normalizedSearch == null || normalizedSearch.isEmpty()
                ? null : "%" + escapeLike(normalizedSearch.toLowerCase(Locale.ROOT)) + "%";

        Page<Long> ids = dishRepository.findPublicDishIds(
                DishStatus.HIDDEN, pattern, categoryId,
                effectiveMinPrice, effectiveMaxPrice, tagId,
                PageRequest.of(page, size, PUBLIC_DISH_SORT)
        );

        if (ids.isEmpty()) {
            return new PageResponse<>(List.of(), ids.getNumber(), ids.getSize(), ids.getTotalElements(),
                    ids.getTotalPages(), ids.isFirst(), ids.isLast());
        }
        Map<Long, Dish> byId = dishRepository.findAllPublicDetailsByIdIn(ids.getContent(), DishStatus.HIDDEN)
                .stream().collect(Collectors.toMap(Dish::getId, Function.identity()));
        return new PageResponse<>(ids.getContent().stream().map(byId::get).map(this::toSummary).toList(),
                ids.getNumber(), ids.getSize(), ids.getTotalElements(), ids.getTotalPages(),
                ids.isFirst(), ids.isLast());
    }

    public DishDetailResponse getDish(Long id) {
        if (id == null || id <= 0) {
            throw badRequest("INVALID_DISH_ID", "Mã món ăn phải lớn hơn 0.");
        }
        Dish dish = dishRepository.findPublicDetailById(id, DishStatus.HIDDEN)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "DISH_NOT_FOUND", "Không tìm thấy món ăn."));
        DishSummaryResponse summary = toSummary(dish);
        return new DishDetailResponse(summary.id(), summary.name(), summary.description(), summary.price(),
                summary.imageUrl(), summary.status(), summary.category(), summary.tags(), dish.getDisplayOrder());
    }

    private void validate(int page, int size, Long categoryId, Long tagId) {
        if (page < 0) throw badRequest("INVALID_PAGE", "Số trang phải lớn hơn hoặc bằng 0.");
        if (size < 1 || size > MAX_PAGE_SIZE) throw badRequest("INVALID_PAGE_SIZE", "Kích thước trang phải từ 1 đến 100.");
        if (categoryId != null && categoryId <= 0) throw badRequest("INVALID_CATEGORY_ID", "Mã danh mục phải lớn hơn 0.");
        if (tagId != null && tagId <= 0) throw badRequest("INVALID_TAG_ID", "Mã nhãn phải lớn hơn 0.");
    }

    private String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private BusinessException badRequest(String code, String message) {
        return new BusinessException(HttpStatus.BAD_REQUEST, code, message);
    }

    private DishSummaryResponse toSummary(Dish dish) {
        var category = dish.getCategory();
        List<PublicDishTagResponse> tags = dish.getTags().stream()
                .sorted(Comparator.comparing(Tag::getName).thenComparing(Tag::getId))
                .map(tag -> new PublicDishTagResponse(tag.getId(), tag.getName())).toList();
        return new DishSummaryResponse(dish.getId(), dish.getName(), dish.getDescription(), dish.getPrice(),
                dish.getImageUrl(), dish.getStatus(),
                new PublicDishCategoryResponse(category.getId(), category.getName(), category.getDisplayOrder()), tags);
    }
}
