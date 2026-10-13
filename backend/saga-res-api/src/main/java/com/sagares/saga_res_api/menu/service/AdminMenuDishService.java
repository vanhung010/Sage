package com.sagares.saga_res_api.menu.service;

import com.sagares.saga_res_api.common.dto.PageResponse;
import com.sagares.saga_res_api.common.exception.BusinessException;
import com.sagares.saga_res_api.common.service.CloudinaryService;
import com.sagares.saga_res_api.menu.dto.admin.AdminDishCategoryResponse;
import com.sagares.saga_res_api.menu.dto.admin.AdminDishResponse;
import com.sagares.saga_res_api.menu.dto.admin.AdminDishTagResponse;
import com.sagares.saga_res_api.menu.dto.admin.CreateDishRequest;
import com.sagares.saga_res_api.menu.dto.admin.UpdateDishRequest;
import com.sagares.saga_res_api.menu.dto.admin.UpdateDishStatusRequest;
import com.sagares.saga_res_api.menu.entity.Category;
import com.sagares.saga_res_api.menu.entity.Dish;
import com.sagares.saga_res_api.menu.entity.DishStatus;
import com.sagares.saga_res_api.menu.entity.Tag;
import com.sagares.saga_res_api.menu.repository.CategoryRepository;
import com.sagares.saga_res_api.menu.repository.DishRepository;
import com.sagares.saga_res_api.menu.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminMenuDishService {

    private static final int DISH_NAME_MAX_LENGTH = 255;
    private static final int MAX_PAGE_SIZE = 100;
    private static final Sort ADMIN_DISH_SORT = Sort.by(
            Sort.Order.asc("displayOrder"),
            Sort.Order.asc("name")
    );

    private final DishRepository dishRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final CloudinaryService cloudinaryService;

    public PageResponse<AdminDishResponse> listDishes(
            int page,
            int size,
            Long categoryId,
            DishStatus status
    ) {
        validatePage(page, size);
        Page<Long> dishIds = dishRepository.findAdminDishIds(
                categoryId,
                status,
                PageRequest.of(page, size, ADMIN_DISH_SORT)
        );

        if (dishIds.isEmpty()) {
            return new PageResponse<>(
                    List.of(),
                    dishIds.getNumber(),
                    dishIds.getSize(),
                    dishIds.getTotalElements(),
                    dishIds.getTotalPages(),
                    dishIds.isFirst(),
                    dishIds.isLast()
            );
        }

        Map<Long, Dish> dishesById = dishRepository.findAllAdminDetailsByIdIn(dishIds.getContent())
                .stream()
                .collect(Collectors.toMap(Dish::getId, Function.identity()));

        return PageResponse.from(dishIds.map(id -> toResponse(dishesById.get(id))));
    }

    public AdminDishResponse getDish(Long id) {
        return toResponse(findDish(id));
    }

    @Transactional
    public AdminDishResponse createDish(CreateDishRequest request, MultipartFile image) {
        String name = normalizeName(request.name());
        validatePrice(request.price());
        int displayOrder = request.displayOrder() == null ? 0 : request.displayOrder();
        validateDisplayOrder(displayOrder);

        Category category = findCategory(request.categoryId());
        Set<Tag> tags = loadTags(request.tagIds());

        Dish dish = new Dish();
        dish.setCategory(category);
        dish.setName(name);
        dish.setDescription(request.description());
        dish.setPrice(request.price());
        dish.setStatus(request.status() == null ? DishStatus.AVAILABLE : request.status());
        dish.setDisplayOrder(displayOrder);
        dish.setTags(tags);

        if (image != null) {
            dish.setImageUrl(cloudinaryService.uploadDishImage(image).url());
        }

        return toResponse(dishRepository.saveAndFlush(dish));
    }

    @Transactional
    public AdminDishResponse updateDish(Long id, UpdateDishRequest request, MultipartFile image) {
        if (isEmptyPatch(request, image)) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "EMPTY_PATCH",
                    "Phải cung cấp ít nhất một trường cần cập nhật."
            );
        }

        Dish dish = findDish(id);

        String name = request.name() == null ? null : normalizeName(request.name());
        if (request.price() != null) {
            validatePrice(request.price());
        }
        if (request.displayOrder() != null) {
            validateDisplayOrder(request.displayOrder());
        }

        Category category = request.categoryId() == null ? null : findCategory(request.categoryId());
        Set<Tag> tags = request.tagIds() == null ? null : loadTags(request.tagIds());

        if (category != null) {
            dish.setCategory(category);
        }
        if (name != null) {
            dish.setName(name);
        }
        if (request.description() != null) {
            dish.setDescription(request.description());
        }
        if (request.price() != null) {
            dish.setPrice(request.price());
        }
        if (request.displayOrder() != null) {
            dish.setDisplayOrder(request.displayOrder());
        }
        if (tags != null) {
            dish.getTags().clear();
            dish.getTags().addAll(tags);
        }
        if (image != null) {
            dish.setImageUrl(cloudinaryService.uploadDishImage(image).url());
        }

        return toResponse(dishRepository.saveAndFlush(dish));
    }

    @Transactional
    public AdminDishResponse updateDishStatus(Long id, UpdateDishStatusRequest request) {
        if (request == null || request.status() == null) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_DISH_STATUS",
                    "Trạng thái món ăn là bắt buộc."
            );
        }

        Dish dish = findDish(id);
        dish.setStatus(request.status());
        return toResponse(dishRepository.saveAndFlush(dish));
    }

    private Dish findDish(Long id) {
        return dishRepository.findAdminDetailById(id)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "DISH_NOT_FOUND",
                        "Không tìm thấy món ăn."
                ));
    }

    private Category findCategory(Long id) {
        if (id == null) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_CATEGORY_ID",
                    "Danh mục món ăn là bắt buộc."
            );
        }
        return categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "CATEGORY_NOT_FOUND",
                        "Không tìm thấy danh mục món ăn."
                ));
    }

    private Set<Tag> loadTags(List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return new HashSet<>();
        }

        Set<Long> uniqueIds = new LinkedHashSet<>(tagIds);
        if (uniqueIds.contains(null)) {
            throw tagNotFound();
        }

        List<Tag> tags = tagRepository.findAllById(uniqueIds);
        if (tags.size() != uniqueIds.size()) {
            throw tagNotFound();
        }
        return new HashSet<>(tags);
    }

    private String normalizeName(String name) {
        String normalized = name == null ? "" : name.trim();
        if (normalized.isBlank()) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_DISH_NAME",
                    "Tên món ăn không được để trống."
            );
        }
        if (normalized.length() > DISH_NAME_MAX_LENGTH) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_DISH_NAME",
                    "Tên món ăn không được vượt quá 255 ký tự."
            );
        }
        return normalized;
    }

    private void validatePrice(Integer price) {
        if (price == null || price <= 0) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_DISH_PRICE",
                    "Giá món ăn phải lớn hơn 0."
            );
        }
    }

    private void validateDisplayOrder(Integer displayOrder) {
        if (displayOrder == null || displayOrder < 0) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_DISPLAY_ORDER",
                    "Thứ tự hiển thị không được nhỏ hơn 0."
            );
        }
    }

    private void validatePage(int page, int size) {
        if (page < 0) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_PAGE",
                    "Số trang không được nhỏ hơn 0."
            );
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_PAGE_SIZE",
                    "Kích thước trang phải từ 1 đến 100."
            );
        }
    }

    private boolean isEmptyPatch(UpdateDishRequest request, MultipartFile image) {
        return request.categoryId() == null
                && request.name() == null
                && request.description() == null
                && request.price() == null
                && request.displayOrder() == null
                && request.tagIds() == null
                && image == null;
    }

    private BusinessException tagNotFound() {
        return new BusinessException(
                HttpStatus.NOT_FOUND,
                "TAG_NOT_FOUND",
                "Không tìm thấy một hoặc nhiều nhãn món ăn."
        );
    }

    private AdminDishResponse toResponse(Dish dish) {
        Category category = dish.getCategory();
        List<AdminDishTagResponse> tags = dish.getTags().stream()
                .sorted(Comparator.comparing(Tag::getName).thenComparing(Tag::getId))
                .map(tag -> new AdminDishTagResponse(tag.getId(), tag.getName()))
                .toList();

        return new AdminDishResponse(
                dish.getId(),
                dish.getName(),
                dish.getDescription(),
                dish.getPrice(),
                dish.getImageUrl(),
                dish.getStatus(),
                dish.getDisplayOrder(),
                new AdminDishCategoryResponse(category.getId(), category.getName()),
                tags,
                dish.getCreatedAt(),
                dish.getUpdatedAt()
        );
    }
}
