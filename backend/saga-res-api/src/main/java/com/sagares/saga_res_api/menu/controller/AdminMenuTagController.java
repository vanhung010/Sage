package com.sagares.saga_res_api.menu.controller;

import com.sagares.saga_res_api.menu.dto.admin.AdminTagResponse;
import com.sagares.saga_res_api.menu.dto.admin.CreateTagRequest;
import com.sagares.saga_res_api.menu.dto.admin.UpdateTagRequest;
import com.sagares.saga_res_api.menu.service.AdminMenuTagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/menu/tags")
@RequiredArgsConstructor
public class AdminMenuTagController {

    private final AdminMenuTagService adminMenuTagService;

    @GetMapping
    public List<AdminTagResponse> getTags() {
        return adminMenuTagService.listTags();
    }

    @PostMapping
    public ResponseEntity<AdminTagResponse> createTag(
            @Valid @RequestBody CreateTagRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminMenuTagService.createTag(request));
    }

    @PatchMapping("/{id}")
    public AdminTagResponse updateTag(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTagRequest request
    ) {
        return adminMenuTagService.updateTag(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTag(@PathVariable Long id) {
        adminMenuTagService.deleteTag(id);
        return ResponseEntity.noContent().build();
    }
}
