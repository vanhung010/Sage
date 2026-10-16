package com.sagares.saga_res_api.menu.service;

import com.sagares.saga_res_api.menu.dto.publicmenu.PublicDishTagResponse;
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
@Import(PublicMenuTagService.class)
class PublicMenuTagPersistenceTest {

    private static final String SCHEMA = "menu_tag_it_" + UUID.randomUUID().toString().replace("-", "");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", () -> System.getProperty("menu-public.test-url", "jdbc:postgresql://127.0.0.1:55439/postgres"));
        properties.add("spring.datasource.username", () -> System.getProperty("menu-public.test-user", "opening_hour_test"));
        properties.add("spring.datasource.password", () -> System.getProperty("menu-public.test-password", ""));
        properties.add("spring.flyway.default-schema", () -> SCHEMA);
        properties.add("spring.jpa.properties.hibernate.default_schema", () -> SCHEMA);
        properties.add("spring.datasource.hikari.connection-init-sql", () -> "SET search_path TO " + SCHEMA + ", public");
    }

    @Autowired PublicMenuTagService service;
    @Autowired JdbcTemplate jdbc;

    @Test
    void returnsAllTagsOrderedByNameEvenWithoutDishes() {
        Long vegan = insertTag("Vegan");
        Long bestseller = insertTag("Bán chạy");
        Long newDish = insertTag("Món mới");

        assertThat(service.listTags()).containsExactly(
                new PublicDishTagResponse(bestseller, "Bán chạy"),
                new PublicDishTagResponse(newDish, "Món mới"),
                new PublicDishTagResponse(vegan, "Vegan"));
    }

    @Test
    void returnsEmptyListWhenNoTagsExist() {
        assertThat(service.listTags()).isEmpty();
    }

    private Long insertTag(String name) {
        return jdbc.queryForObject("INSERT INTO tag (name) VALUES (?) RETURNING id", Long.class, name);
    }
}
