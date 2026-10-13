package com.sagares.saga_res_api.menu.controller;

import com.sagares.saga_res_api.menu.dto.admin.AdminTagResponse;
import com.sagares.saga_res_api.menu.dto.admin.CreateTagRequest;
import com.sagares.saga_res_api.menu.dto.admin.UpdateTagRequest;
import com.sagares.saga_res_api.menu.service.AdminMenuTagService;
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
@Tag(
        name = "Admin Menu Tags",
        description = "Manage reusable dish tags. Access is restricted to ADMIN."
)
public class AdminMenuTagController {

    private final AdminMenuTagService adminMenuTagService;

    @GetMapping
    @Operation(
            summary = "List menu tags",
            description = "Returns all menu tags ordered alphabetically by name. Access: ADMIN."
    )
    @ApiResponse(responseCode = "200", description = "Tags returned", useReturnTypeSchema = true)
    public List<AdminTagResponse> getTags() {
        return adminMenuTagService.listTags();
    }

    @PostMapping
    @Operation(
            summary = "Create a menu tag",
            description = "Creates a reusable dish tag after trimming and validating its unique name. Access: ADMIN."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tag created", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "Malformed JSON, field validation failure, or a blank normalized name",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "A tag with the same name already exists",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AdminTagResponse> createTag(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "New tag values.", required = true,
                    content = @Content(schema = @Schema(implementation = CreateTagRequest.class),
                            examples = @ExampleObject(value = "{\"name\":\"Best Seller\"}"))
            )
            @Valid @RequestBody CreateTagRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminMenuTagService.createTag(request));
    }

    @PatchMapping("/{id}")
    @Operation(
            summary = "Rename a menu tag",
            description = "Changes a tag's name. The name field is required and remains unique after trimming. Access: ADMIN."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tag updated", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "Invalid path ID, malformed JSON, field validation failure, blank normalized name, or missing name field",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Tag not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "The updated name conflicts with another tag",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public AdminTagResponse updateTag(
            @Parameter(description = "ID of the tag to update.", example = "10", required = true)
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "New tag name.", required = true,
                    content = @Content(schema = @Schema(implementation = UpdateTagRequest.class),
                            examples = @ExampleObject(value = "{\"name\":\"Chef's Choice\"}"))
            )
            @Valid @RequestBody UpdateTagRequest request
    ) {
        return adminMenuTagService.updateTag(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete an unused menu tag",
            description = "Permanently deletes a tag only when no dish references it. Access: ADMIN."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Tag deleted"),
            @ApiResponse(responseCode = "400", description = "The path ID is not a valid long integer",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Tag not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "The tag is assigned to one or more dishes or another data-integrity conflict prevents deletion",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteTag(
            @Parameter(description = "ID of the tag to delete.", example = "10", required = true)
            @PathVariable Long id) {
        adminMenuTagService.deleteTag(id);
        return ResponseEntity.noContent().build();
    }
}
