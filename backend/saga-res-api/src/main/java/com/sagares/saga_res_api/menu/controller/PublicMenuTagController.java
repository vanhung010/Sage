package com.sagares.saga_res_api.menu.controller;

import com.sagares.saga_res_api.menu.dto.publicmenu.PublicDishTagResponse;
import com.sagares.saga_res_api.menu.service.PublicMenuTagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/menu/tags")
@RequiredArgsConstructor
@Tag(name = "Menu", description = "Public menu browsing")
public class PublicMenuTagController {

    private final PublicMenuTagService service;

    @GetMapping
    @Operation(summary = "List tags available for public menu filtering",
            description = "Returns all tags ordered by name ascending, including tags not assigned to dishes. Returns an empty array when none exist.")
    public List<PublicDishTagResponse> listTags() {
        return service.listTags();
    }
}
