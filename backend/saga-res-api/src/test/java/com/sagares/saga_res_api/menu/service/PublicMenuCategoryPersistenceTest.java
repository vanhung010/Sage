package com.sagares.saga_res_api.menu.service;

import com.sagares.saga_res_api.menu.dto.publicmenu.PublicDishCategoryResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfSystemProperty(named = "menu-public.integration", matches = "true")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(PublicMenuCategoryService.class)
class PublicMenuCategoryPersistenceTest {

    private static final String SCHEMA = "menu_category_it_" + UUID.randomUUID().toString().replace("-", "");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", () -> System.getProperty("menu-public.test-url", "jdbc:postgresql://127.0.0.1:55439/postgres"));
        properties.add("spring.datasource.username", () -> System.getProperty("menu-public.test-user", "opening_hour_test"));
        properties.add("spring.datasource.password", () -> System.getProperty("menu-public.test-password", ""));
        properties.add("spring.flyway.default-schema", () -> SCHEMA);
        properties.add("spring.jpa.properties.hibernate.default_schema", () -> SCHEMA);
        properties.add("spring.datasource.hikari.connection-init-sql", () -> "SET search_path TO " + SCHEMA + ", public");
    }

    @Autowired PublicMenuCategoryService service;
    @Autowired JdbcTemplate jdbc;

    @Test
    void returnsOnlyActiveCategoriesOrderedByDisplayOrderThenNameEvenWithoutDishes() {
        Long later = insertCategory("Alpha", 2, true);
        Long second = insertCategory("Zulu", 1, true);
        insertCategory("Hidden", 0, false);
        Long first = insertCategory("Beta", 1, true);

        assertThat(service.listCategories()).containsExactly(
                new PublicDishCategoryResponse(first, "Beta", 1),
                new PublicDishCategoryResponse(second, "Zulu", 1),
                new PublicDishCategoryResponse(later, "Alpha", 2));
    }

    @Test
    void returnsEmptyListWhenAllCategoriesAreInactive() {
        insertCategory("Inactive", 1, false);

        assertThat(service.listCategories()).isEmpty();
    }

    @Test
    void returnsEmptyListWhenNoCategoriesExist() {
        assertThat(service.listCategories()).isEmpty();
    }

    private Long insertCategory(String name, int displayOrder, boolean active) {
        return jdbc.queryForObject("""
                INSERT INTO category (name, display_order, is_active) VALUES (?, ?, ?) RETURNING id
                """, Long.class, name, displayOrder, active);
    }
}
