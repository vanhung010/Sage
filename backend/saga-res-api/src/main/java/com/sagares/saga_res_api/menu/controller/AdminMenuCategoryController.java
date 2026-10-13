package com.sagares.saga_res_api.menu.controller;

import com.sagares.saga_res_api.menu.dto.admin.AdminCategoryResponse;
import com.sagares.saga_res_api.menu.dto.admin.CreateCategoryRequest;
import com.sagares.saga_res_api.menu.dto.admin.UpdateCategoryRequest;
import com.sagares.saga_res_api.menu.service.AdminMenuCategoryService;
import com.sagares.saga_res_api.common.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/menu/categories")
@RequiredArgsConstructor
@Tag(
        name = "Admin Menu Categories",
        description = "Manage menu category ordering and active state. Access is restricted to ADMIN."
)
public class AdminMenuCategoryController {

    private final AdminMenuCategoryService adminMenuCategoryService;

    @GetMapping
    @Operation(
            summary = "List menu categories",
            description = "Returns every category, including inactive entries, ordered by displayOrder and then name. Access: ADMIN."
    )
    @ApiResponse(responseCode = "200", description = "Categories returned", useReturnTypeSchema = true)
    public List<AdminCategoryResponse> getCategories() {

        return adminMenuCategoryService.listCategories();
    }

    @PostMapping
    @Operation(
            summary = "Create a menu category",
            description = "Creates an active category after trimming and validating its unique name. Access: ADMIN."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Category created", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "Malformed JSON, field validation failure, or a blank normalized name",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "A category with the same name already exists",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AdminCategoryResponse> createCategory(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "New category values.", required = true,
                    content = @Content(schema = @Schema(implementation = CreateCategoryRequest.class),
                            examples = @ExampleObject(value = "{\"name\":\"Main Course\",\"displayOrder\":2}"))
            )
            @Valid @RequestBody CreateCategoryRequest request
    ) {
        AdminCategoryResponse response = adminMenuCategoryService.createCategory(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PatchMapping("/{id}")
    @Operation(
            summary = "Update a menu category",
            description = "Partially updates a category's name, display order, or active state. At least one field is required. Access: ADMIN."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Category updated", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "Invalid path ID, malformed JSON, field validation failure, invalid normalized name, or empty patch",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Category not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "The updated name conflicts with another category",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public AdminCategoryResponse updateCategory(
            @Parameter(description = "ID of the category to update.", example = "10", required = true)
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Category fields to change.", required = true,
                    content = @Content(schema = @Schema(implementation = UpdateCategoryRequest.class),
                            examples = @ExampleObject(value = "{\"name\":\"Main Courses\",\"displayOrder\":3,\"active\":true}"))
            )
            @Valid @RequestBody UpdateCategoryRequest request
    ) {
        return adminMenuCategoryService.updateCategory(id, request);
    }
}
