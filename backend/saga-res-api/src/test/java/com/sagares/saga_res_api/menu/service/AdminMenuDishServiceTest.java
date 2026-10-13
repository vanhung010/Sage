package com.sagares.saga_res_api.menu.service;

import com.sagares.saga_res_api.common.exception.BusinessException;
import com.sagares.saga_res_api.common.service.CloudinaryService;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminMenuDishServiceTest {

    @Mock
    private DishRepository dishRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TagRepository tagRepository;

    @Mock
    private CloudinaryService cloudinaryService;

    @InjectMocks
    private AdminMenuDishService service;

    @Test
    void createWithoutImageUsesDefaultsAndNormalizesDuplicateTags() {
        Category category = category(1L, "Món chính");
        Tag tag = tag(2L, "Đặc sắc");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(tagRepository.findAllById(any())).thenReturn(List.of(tag));
        stubSave();

        var response = service.createDish(
                new CreateDishRequest(1L, "  Cá kho  ", "Món Việt", 120_000, null, null,
                        List.of(2L, 2L)),
                null
        );

        assertThat(response.name()).isEqualTo("Cá kho");
        assertThat(response.status()).isEqualTo(DishStatus.AVAILABLE);
        assertThat(response.displayOrder()).isZero();
        assertThat(response.imageUrl()).isNull();
        assertThat(response.tags()).extracting("id").containsExactly(2L);
        verifyNoInteractions(cloudinaryService);
    }

    @Test
    void createWithImageUploadsAndStoresReturnedUrl() {
        MockMultipartFile image = image();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category(1L, "Món chính")));
        when(cloudinaryService.uploadDishImage(image))
                .thenReturn(new CloudinaryService.UploadResult("https://cdn.example/dish.jpg", "dish-id"));
        stubSave();

        var response = service.createDish(createRequest(), image);

        assertThat(response.imageUrl()).isEqualTo("https://cdn.example/dish.jpg");
        verify(cloudinaryService).uploadDishImage(image);
    }

    @Test
    void createRejectsUnknownCategory() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertError(
                () -> service.createDish(new CreateDishRequest(99L, "Cá kho", null, 1000, null, null, null), null),
                HttpStatus.NOT_FOUND,
                "CATEGORY_NOT_FOUND"
        );
        verify(dishRepository, never()).saveAndFlush(any());
    }

    @Test
    void createRejectsAnyUnknownTag() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category(1L, "Món chính")));
        when(tagRepository.findAllById(any())).thenReturn(List.of(tag(2L, "Đặc sắc")));

        assertError(
                () -> service.createDish(new CreateDishRequest(1L, "Cá kho", null, 1000, null, null,
                        List.of(2L, 3L)), null),
                HttpStatus.NOT_FOUND,
                "TAG_NOT_FOUND"
        );
    }

    @Test
    void createRejectsInvalidPriceBeforeDatabaseWork() {
        assertError(
                () -> service.createDish(new CreateDishRequest(1L, "Cá kho", null, 0, null, null, null), null),
                HttpStatus.BAD_REQUEST,
                "INVALID_DISH_PRICE"
        );
        verifyNoInteractions(categoryRepository, tagRepository, dishRepository, cloudinaryService);
    }

    @Test
    void createRejectsBlankNameBeforeDatabaseWork() {
        assertError(
                () -> service.createDish(new CreateDishRequest(1L, "   ", null, 1000, null, null, null), null),
                HttpStatus.BAD_REQUEST,
                "INVALID_DISH_NAME"
        );
        verifyNoInteractions(categoryRepository, tagRepository, dishRepository, cloudinaryService);
    }

    @Test
    void updateNamePriceAndCategory() {
        Dish dish = dish(1L, DishStatus.AVAILABLE);
        Category newCategory = category(2L, "Hải sản");
        when(dishRepository.findAdminDetailById(1L)).thenReturn(Optional.of(dish));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(newCategory));
        when(dishRepository.saveAndFlush(dish)).thenReturn(dish);

        var response = service.updateDish(
                1L,
                new UpdateDishRequest(2L, "  Tôm nướng  ", null, 180_000, null, null),
                null
        );

        assertThat(response.name()).isEqualTo("Tôm nướng");
        assertThat(response.price()).isEqualTo(180_000);
        assertThat(response.category().id()).isEqualTo(2L);
        assertThat(response.status()).isEqualTo(DishStatus.AVAILABLE);
    }

    @Test
    void updateReplacesTagsAndNormalizesDuplicates() {
        Dish dish = dish(1L, DishStatus.AVAILABLE);
        Tag spicy = tag(2L, "Cay");
        Tag popular = tag(3L, "Phổ biến");
        when(dishRepository.findAdminDetailById(1L)).thenReturn(Optional.of(dish));
        when(tagRepository.findAllById(any())).thenReturn(List.of(spicy, popular));
        when(dishRepository.saveAndFlush(dish)).thenReturn(dish);

        var response = service.updateDish(
                1L,
                new UpdateDishRequest(null, null, null, null, null, List.of(2L, 3L, 2L)),
                null
        );

        assertThat(response.tags()).extracting("id").containsExactlyInAnyOrder(2L, 3L);
    }

    @Test
    void emptyTagIdsRemovesAllTags() {
        Dish dish = dish(1L, DishStatus.AVAILABLE);
        dish.setTags(new HashSet<>(List.of(tag(2L, "Cay"))));
        when(dishRepository.findAdminDetailById(1L)).thenReturn(Optional.of(dish));
        when(dishRepository.saveAndFlush(dish)).thenReturn(dish);

        var response = service.updateDish(
                1L,
                new UpdateDishRequest(null, null, null, null, null, List.of()),
                null
        );

        assertThat(response.tags()).isEmpty();
        verifyNoInteractions(tagRepository);
    }

    @Test
    void nullTagIdsKeepsCurrentTags() {
        Dish dish = dish(1L, DishStatus.AVAILABLE);
        dish.setTags(new HashSet<>(List.of(tag(2L, "Cay"))));
        when(dishRepository.findAdminDetailById(1L)).thenReturn(Optional.of(dish));
        when(dishRepository.saveAndFlush(dish)).thenReturn(dish);

        var response = service.updateDish(
                1L,
                new UpdateDishRequest(null, "Tên mới", null, null, null, null),
                null
        );

        assertThat(response.tags()).extracting("id").containsExactly(2L);
        verifyNoInteractions(tagRepository);
    }

    @Test
    void updateWithImageReplacesImageUrl() {
        Dish dish = dish(1L, DishStatus.AVAILABLE);
        dish.setImageUrl("https://cdn.example/old.jpg");
        MockMultipartFile image = image();
        when(dishRepository.findAdminDetailById(1L)).thenReturn(Optional.of(dish));
        when(cloudinaryService.uploadDishImage(image))
                .thenReturn(new CloudinaryService.UploadResult("https://cdn.example/new.jpg", "new-id"));
        when(dishRepository.saveAndFlush(dish)).thenReturn(dish);

        var response = service.updateDish(
                1L,
                new UpdateDishRequest(null, null, null, null, null, null),
                image
        );

        assertThat(response.imageUrl()).isEqualTo("https://cdn.example/new.jpg");
    }

    @Test
    void updateRejectsEmptyPatch() {
        assertError(
                () -> service.updateDish(1L, new UpdateDishRequest(null, null, null, null, null, null), null),
                HttpStatus.BAD_REQUEST,
                "EMPTY_PATCH"
        );
        verifyNoInteractions(dishRepository, categoryRepository, tagRepository, cloudinaryService);
    }

    @Test
    void updateRejectsUnknownDish() {
        when(dishRepository.findAdminDetailById(99L)).thenReturn(Optional.empty());

        assertError(
                () -> service.updateDish(99L,
                        new UpdateDishRequest(null, "Tên mới", null, null, null, null), null),
                HttpStatus.NOT_FOUND,
                "DISH_NOT_FOUND"
        );
    }

    @Test
    void statusCanChangeBetweenAllSupportedAdminStates() {
        Dish dish = dish(1L, DishStatus.AVAILABLE);
        when(dishRepository.findAdminDetailById(1L)).thenReturn(Optional.of(dish));
        when(dishRepository.saveAndFlush(dish)).thenReturn(dish);

        assertThat(service.updateDishStatus(1L, new UpdateDishStatusRequest(DishStatus.SOLD_OUT)).status())
                .isEqualTo(DishStatus.SOLD_OUT);
        assertThat(service.updateDishStatus(1L, new UpdateDishStatusRequest(DishStatus.AVAILABLE)).status())
                .isEqualTo(DishStatus.AVAILABLE);
        assertThat(service.updateDishStatus(1L, new UpdateDishStatusRequest(DishStatus.HIDDEN)).status())
                .isEqualTo(DishStatus.HIDDEN);
    }

    @Test
    void statusUpdateRejectsUnknownDish() {
        when(dishRepository.findAdminDetailById(99L)).thenReturn(Optional.empty());

        assertError(
                () -> service.updateDishStatus(99L, new UpdateDishStatusRequest(DishStatus.HIDDEN)),
                HttpStatus.NOT_FOUND,
                "DISH_NOT_FOUND"
        );
    }

    @Test
    void listIncludesHiddenAppliesFiltersAndPreservesPaginationOrder() {
        Dish available = dish(1L, DishStatus.AVAILABLE);
        Dish hidden = dish(2L, DishStatus.HIDDEN);
        when(dishRepository.findAdminDishIds(any(), any(), any())).thenAnswer(invocation -> {
            Pageable requested = invocation.getArgument(2);
            return new PageImpl<>(List.of(2L, 1L), requested, 3);
        });
        when(dishRepository.findAllAdminDetailsByIdIn(List.of(2L, 1L)))
                .thenReturn(List.of(available, hidden));

        var response = service.listDishes(0, 2, 5L, null);

        assertThat(response.content()).extracting("id").containsExactly(2L, 1L);
        assertThat(response.content()).extracting("status")
                .containsExactly(DishStatus.HIDDEN, DishStatus.AVAILABLE);
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(2);
        assertThat(response.totalElements()).isEqualTo(3);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(dishRepository).findAdminDishIds(any(), any(), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("displayOrder")).isNotNull();
        verify(dishRepository).findAdminDishIds(5L, null, pageableCaptor.getValue());
    }

    @Test
    void listPassesStatusFilterAndRejectsOversizedPage() {
        when(dishRepository.findAdminDishIds(any(), any(), any())).thenAnswer(invocation ->
                new PageImpl<Long>(List.of(), invocation.getArgument(2), 0));

        var response = service.listDishes(0, 20, null, DishStatus.HIDDEN);

        assertThat(response.content()).isEmpty();
        verify(dishRepository).findAdminDishIds(eq(null), eq(DishStatus.HIDDEN), any());
        assertError(() -> service.listDishes(0, 101, null, null),
                HttpStatus.BAD_REQUEST, "INVALID_PAGE_SIZE");
    }

    @Test
    void detailReturnsCategoryAndSortedTags() {
        Dish dish = dish(1L, DishStatus.HIDDEN);
        dish.setTags(new HashSet<>(List.of(tag(3L, "Phổ biến"), tag(2L, "Cay"))));
        when(dishRepository.findAdminDetailById(1L)).thenReturn(Optional.of(dish));

        var response = service.getDish(1L);

        assertThat(response.category().name()).isEqualTo("Món chính");
        assertThat(response.tags()).extracting("name").containsExactly("Cay", "Phổ biến");
        assertThat(response.createdAt()).isNotNull();
        assertThat(response.updatedAt()).isNotNull();
    }

    private void stubSave() {
        when(dishRepository.saveAndFlush(any(Dish.class))).thenAnswer(invocation -> {
            Dish saved = invocation.getArgument(0);
            saved.setId(1L);
            saved.setCreatedAt(Instant.parse("2026-10-13T00:00:00Z"));
            saved.setUpdatedAt(Instant.parse("2026-10-13T00:00:00Z"));
            return saved;
        });
    }

    private CreateDishRequest createRequest() {
        return new CreateDishRequest(1L, "Cá kho", null, 120_000, null, null, null);
    }

    private Category category(Long id, String name) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        return category;
    }

    private Tag tag(Long id, String name) {
        Tag tag = new Tag();
        tag.setId(id);
        tag.setName(name);
        return tag;
    }

    private Dish dish(Long id, DishStatus status) {
        Dish dish = new Dish();
        dish.setId(id);
        dish.setCategory(category(1L, "Món chính"));
        dish.setName("Cá kho");
        dish.setDescription("Món Việt");
        dish.setPrice(120_000);
        dish.setStatus(status);
        dish.setDisplayOrder(1);
        dish.setTags(new HashSet<>());
        dish.setCreatedAt(Instant.parse("2026-10-13T00:00:00Z"));
        dish.setUpdatedAt(Instant.parse("2026-10-13T01:00:00Z"));
        return dish;
    }

    private MockMultipartFile image() {
        return new MockMultipartFile("image", "dish.jpg", "image/jpeg", new byte[]{1, 2, 3});
    }

    private void assertError(Runnable operation, HttpStatus status, String code) {
        assertThatThrownBy(operation::run)
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(status);
                    assertThat(exception.getCode()).isEqualTo(code);
                    assertThat(exception.getMessage()).isNotBlank();
                });
    }
}
