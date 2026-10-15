package com.sagares.saga_res_api.menu.service;

import com.sagares.saga_res_api.common.exception.BusinessException;
import com.sagares.saga_res_api.menu.entity.Category;
import com.sagares.saga_res_api.menu.entity.Dish;
import com.sagares.saga_res_api.menu.entity.DishStatus;
import com.sagares.saga_res_api.menu.entity.Tag;
import com.sagares.saga_res_api.menu.repository.DishRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicMenuDishServiceTest {
    @Mock DishRepository repository;
    @InjectMocks PublicMenuDishService service;



    @Test
    void detailHidesHiddenAndMapsDisplayOrder() {
        Dish dish = dish(1L, "Phở", Set.of());
        dish.setDisplayOrder(7);
        when(repository.findPublicDetailById(1L, DishStatus.HIDDEN)).thenReturn(Optional.of(dish));
        var response = service.getDish(1L);
        assertThat(response.displayOrder()).isEqualTo(7);
        when(repository.findPublicDetailById(2L, DishStatus.HIDDEN)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getDish(2L)).isInstanceOfSatisfying(BusinessException.class, ex -> {
            assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(ex.getCode()).isEqualTo("DISH_NOT_FOUND");
        });
    }

    private void assertBadRequest(org.assertj.core.api.ThrowableAssert.ThrowingCallable action, String code) {
        assertThatThrownBy(action).isInstanceOfSatisfying(BusinessException.class, ex -> {
            assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(ex.getCode()).isEqualTo(code);
        });
    }

    private Dish dish(Long id, String name, Set<Tag> tags) {
        Category category = new Category(); category.setId(2L); category.setName("Món chính"); category.setDisplayOrder(1);
        Dish dish = new Dish(); dish.setId(id); dish.setName(name); dish.setDescription("Full description");
        dish.setPrice(150000); dish.setImageUrl(null); dish.setStatus(DishStatus.AVAILABLE); dish.setCategory(category);
        dish.setTags(new HashSet<>(tags));
        return dish;
    }
    private Tag tag(Long id, String name) { Tag tag = new Tag(); tag.setId(id); tag.setName(name); return tag; }
}
