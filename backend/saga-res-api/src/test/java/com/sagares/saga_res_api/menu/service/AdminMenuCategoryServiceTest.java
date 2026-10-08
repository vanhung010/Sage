package com.sagares.saga_res_api.menu.service;

import com.sagares.saga_res_api.common.exception.BusinessException;
import com.sagares.saga_res_api.menu.dto.admin.CreateCategoryRequest;
import com.sagares.saga_res_api.menu.dto.admin.UpdateCategoryRequest;
import com.sagares.saga_res_api.menu.entity.Category;
import com.sagares.saga_res_api.menu.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminMenuCategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private AdminMenuCategoryService service;

    @Test
    void listCategoriesUsesAdminOrderingAndIncludesInactiveRows() {
        Category active = category(1L, "A", 1, true);
        Category inactive = category(2L, "B", 2, false);
        when(categoryRepository.findAllByOrderByDisplayOrderAscNameAsc())
                .thenReturn(List.of(active, inactive));

        var responses = service.listCategories();

        assertThat(responses).extracting("id").containsExactly(1L, 2L);
        assertThat(responses).extracting("active").containsExactly(true, false);
        verify(categoryRepository).findAllByOrderByDisplayOrderAscNameAsc();
    }

    @Test
    void createCategoryTrimsNameAndDefaultsActiveToTrue() {
        when(categoryRepository.existsByName("Khai vị")).thenReturn(false);
        when(categoryRepository.saveAndFlush(any(Category.class))).thenAnswer(invocation -> {
            Category saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        var response = service.createCategory(new CreateCategoryRequest("  Khai vị  ", 1));

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Khai vị");
        assertThat(response.displayOrder()).isEqualTo(1);
        assertThat(response.active()).isTrue();
    }

    @Test
    void createCategoryRejectsDuplicateName() {
        when(categoryRepository.existsByName("Khai vị")).thenReturn(true);

        assertThatThrownBy(() -> service.createCategory(new CreateCategoryRequest("Khai vị", 1)))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.getCode()).isEqualTo("DUPLICATE_CATEGORY_NAME");
                });
    }

    @Test
    void createCategoryMapsDatabaseUniqueRaceToConflict() {
        when(categoryRepository.existsByName("Khai vị")).thenReturn(false);
        when(categoryRepository.saveAndFlush(any(Category.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThatThrownBy(() -> service.createCategory(new CreateCategoryRequest("Khai vị", 1)))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void updateCategoryChangesOnlySuppliedFields() {
        Category category = category(1L, "Khai vị", 1, true);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.saveAndFlush(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.updateCategory(1L, new UpdateCategoryRequest(null, 2, null));

        assertThat(response.name()).isEqualTo("Khai vị");
        assertThat(response.displayOrder()).isEqualTo(2);
        assertThat(response.active()).isTrue();
    }

    @Test
    void updateCategoryCanDeactivateCategory() {
        Category category = category(1L, "Khai vị", 1, true);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.saveAndFlush(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.updateCategory(1L, new UpdateCategoryRequest(null, null, false));

        assertThat(response.active()).isFalse();
    }

    @Test
    void updateCategoryRejectsUnknownId() {
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateCategory(999L, new UpdateCategoryRequest("Món khai vị", null, null)))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(exception.getCode()).isEqualTo("CATEGORY_NOT_FOUND");
                });
    }

    @Test
    void updateCategoryRejectsDuplicateName() {
        Category category = category(1L, "Khai vị", 1, true);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByNameAndIdNot("Món khai vị", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.updateCategory(1L, new UpdateCategoryRequest("Món khai vị", null, null)))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.getCode()).isEqualTo("DUPLICATE_CATEGORY_NAME");
                });
    }

    @Test
    void updateCategoryRejectsEmptyPatch() {
        assertThatThrownBy(() -> service.updateCategory(1L, new UpdateCategoryRequest(null, null, null)))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(exception.getCode()).isEqualTo("EMPTY_PATCH");
                });
    }

    @Test
    void updateCategoryRejectsBlankSuppliedNameAfterTrim() {
        Category category = category(1L, "Khai vị", 1, true);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> service.updateCategory(1L, new UpdateCategoryRequest("   ", null, null)))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    private Category category(Long id, String name, int displayOrder, boolean active) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setDisplayOrder(displayOrder);
        category.setActive(active);
        return category;
    }
}
