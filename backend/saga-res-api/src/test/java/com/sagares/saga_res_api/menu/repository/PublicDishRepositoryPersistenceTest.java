package com.sagares.saga_res_api.menu.repository;

import com.sagares.saga_res_api.menu.entity.DishStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfSystemProperty(named = "menu-public.integration", matches = "true")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PublicDishRepositoryPersistenceTest {
    private static final String SCHEMA = "menu_public_it_" + UUID.randomUUID().toString().replace("-", "");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", () -> System.getProperty("menu-public.test-url", "jdbc:postgresql://127.0.0.1:55439/postgres"));
        properties.add("spring.datasource.username", () -> System.getProperty("menu-public.test-user", "opening_hour_test"));
        properties.add("spring.datasource.password", () -> System.getProperty("menu-public.test-password", ""));
        properties.add("spring.flyway.default-schema", () -> SCHEMA);
        properties.add("spring.jpa.properties.hibernate.default_schema", () -> SCHEMA);
        properties.add("spring.datasource.hikari.connection-init-sql", () -> "SET search_path TO " + SCHEMA + ", public");
    }

    @Autowired DishRepository repository;
    @Autowired JdbcTemplate jdbc;

    @Test
    void publicPagingFiltersBeforeCountAndTagFilterDoesNotRestrictFetchedTags() {
        Long category = jdbc.queryForObject("INSERT INTO category (name) VALUES ('Public') RETURNING id", Long.class);
        Long spicy = jdbc.queryForObject("INSERT INTO tag (name) VALUES ('Spicy') RETURNING id", Long.class);
        jdbc.update("INSERT INTO tag (name) VALUES ('Vegan')");
        Long vegan = jdbc.queryForObject("SELECT id FROM tag WHERE name = 'Vegan'", Long.class);
        Long visible = insertDish(category, "A_100% Bowl", "AVAILABLE", 100);
        Long soldOut = insertDish(category, "Another Bowl", "SOLD_OUT", 200);
        insertDish(category, "A_100% Hidden", "HIDDEN", 100);
        jdbc.update("INSERT INTO dish_tag (dish_id, tag_id) VALUES (?, ?), (?, ?)", visible, spicy, visible, vegan);
        jdbc.update("INSERT INTO dish_tag (dish_id, tag_id) VALUES (?, ?)", soldOut, spicy);

        var ids = repository.findPublicDishIds(DishStatus.HIDDEN, "%a\\_100\\%%", category,
                100, 100, spicy, PageRequest.of(0, 1, Sort.by("displayOrder").ascending().and(Sort.by("name").ascending()).and(Sort.by("id").ascending())));
        assertThat(ids.getContent()).containsExactly(visible);
        assertThat(ids.getTotalElements()).isEqualTo(1);
        var fetched = repository.findAllPublicDetailsByIdIn(ids.getContent(), DishStatus.HIDDEN);
        assertThat(fetched).hasSize(1);
        assertThat(fetched.getFirst().getTags()).extracting("name").containsExactlyInAnyOrder("Spicy", "Vegan");

        var all = repository.findPublicDishIds(DishStatus.HIDDEN, null, category, null, null, null,
                PageRequest.of(0, 1, Sort.by("displayOrder").ascending().and(Sort.by("name").ascending()).and(Sort.by("id").ascending())));
        assertThat(all.getTotalElements()).isEqualTo(2);
        assertThat(all.getContent()).hasSize(1);
        assertThat(repository.findAllPublicDetailsByIdIn(java.util.List.of(visible), DishStatus.HIDDEN)).hasSize(1);
    }

    private Long insertDish(Long category, String name, String status, int price) {
        return jdbc.queryForObject("""
                INSERT INTO dish (category_id, name, price, status) VALUES (?, ?, ?, ?) RETURNING id
                """, Long.class, category, name, price, status);
    }
}
