package com.sagares.saga_res_api.menu.service;

import com.sagares.saga_res_api.common.exception.BusinessException;
import com.sagares.saga_res_api.menu.dto.admin.CreateTagRequest;
import com.sagares.saga_res_api.menu.dto.admin.UpdateTagRequest;
import com.sagares.saga_res_api.menu.repository.DishRepository;
import com.sagares.saga_res_api.menu.repository.TagRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@EnabledIfSystemProperty(named = "menu-tag.integration", matches = "true")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(AdminMenuTagService.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AdminMenuTagPersistenceTest {

    private static final String SCHEMA = "menu_tag_it_" + UUID.randomUUID().toString().replace("-", "");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", () -> System.getProperty("menu-tag.test-url",
                "jdbc:postgresql://127.0.0.1:55439/postgres"));
        properties.add("spring.datasource.username", () -> System.getProperty("menu-tag.test-user", "opening_hour_test"));
        properties.add("spring.datasource.password", () -> System.getProperty("menu-tag.test-password", ""));
        properties.add("spring.flyway.default-schema", () -> SCHEMA);
        properties.add("spring.jpa.properties.hibernate.default_schema", () -> SCHEMA);
        properties.add("spring.datasource.hikari.connection-init-sql", () -> "SET search_path TO " + SCHEMA + ", public");
    }

    @Autowired
    private AdminMenuTagService service;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private DishRepository dishRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void createListUpdateAndDeleteUnusedTagPersist() {
        var vegetarian = service.createTag(new CreateTagRequest("  Vegetarian  "));
        var spicy = service.createTag(new CreateTagRequest("Spicy"));

        assertThat(service.listTags()).extracting("name")
                .containsExactly("Spicy", "Vegetarian");

        var updated = service.updateTag(spicy.id(), new UpdateTagRequest("Very Spicy"));
        assertThat(updated.name()).isEqualTo("Very Spicy");
        assertThat(tagRepository.findById(spicy.id()).orElseThrow().getName()).isEqualTo("Very Spicy");

        service.deleteTag(vegetarian.id());
        assertThat(tagRepository.existsById(vegetarian.id())).isFalse();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tag WHERE id = ?", Integer.class, vegetarian.id()))
                .isZero();
    }

    @Test
    void deleteTagInUseKeepsTagAndDishTagRelationship() {
        var tag = service.createTag(new CreateTagRequest("Seafood"));
        Long categoryId = jdbc.queryForObject(
                "INSERT INTO category (name, display_order) VALUES ('Main', 1) RETURNING id",
                Long.class
        );
        Long dishId = jdbc.queryForObject("""
                INSERT INTO dish (category_id, name, price, status, display_order)
                VALUES (?, 'Grilled shrimp', 150000, 'AVAILABLE', 1)
                RETURNING id
                """, Long.class, categoryId);
        jdbc.update("INSERT INTO dish_tag (dish_id, tag_id) VALUES (?, ?)", dishId, tag.id());

        assertThat(dishRepository.existsByTags_Id(tag.id())).isTrue();
        assertThatThrownBy(() -> service.deleteTag(tag.id()))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("TAG_IN_USE");
                    assertThat(exception.getMessage()).isEqualTo("Không thể xóa nhãn đang được gán cho món ăn");
                });

        assertThat(tagRepository.existsById(tag.id())).isTrue();
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM dish_tag WHERE dish_id = ? AND tag_id = ?",
                Integer.class,
                dishId,
                tag.id()
        )).isEqualTo(1);
    }
}
