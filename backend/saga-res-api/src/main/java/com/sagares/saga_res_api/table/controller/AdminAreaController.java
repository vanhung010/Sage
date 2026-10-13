package com.sagares.saga_res_api.table.controller;

import com.sagares.saga_res_api.table.dto.admin.AdminAreaResponse;
import com.sagares.saga_res_api.table.dto.admin.CreateAreaRequest;
import com.sagares.saga_res_api.table.dto.admin.UpdateAreaRequest;
import com.sagares.saga_res_api.table.service.AdminAreaService;
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
@RequestMapping("/api/admin/areas")
@RequiredArgsConstructor
@Tag(
        name = "Admin Areas",
        description = "Manage restaurant seating areas. Access is restricted to ADMIN."
)
public class AdminAreaController {

    private final AdminAreaService adminAreaService;

    @GetMapping
    @Operation(
            summary = "List restaurant areas",
            description = "Returns all areas, including inactive entries, ordered by name. Access: ADMIN."
    )
    @ApiResponse(responseCode = "200", description = "Areas returned", useReturnTypeSchema = true)
    public List<AdminAreaResponse> getAreas() {
        return adminAreaService.listAreas();
    }

    @PostMapping
    @Operation(
            summary = "Create a restaurant area",
            description = "Creates an active seating area after trimming and validating its unique name. Access: ADMIN."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Area created", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "Malformed JSON, field validation failure, or a blank normalized name",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "An area with the same name already exists",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AdminAreaResponse> createArea(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "New area values.", required = true,
                    content = @Content(schema = @Schema(implementation = CreateAreaRequest.class),
                            examples = @ExampleObject(value = "{\"name\":\"Garden\",\"description\":\"Outdoor seating with garden views\"}"))
            )
            @Valid @RequestBody CreateAreaRequest request) {
        AdminAreaResponse response = adminAreaService.createArea(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PatchMapping("/{id}")
    @Operation(
            summary = "Update a restaurant area",
            description = "Partially changes an area's name or description. At least one field is required. Access: ADMIN."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Area updated", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "Invalid path ID, malformed JSON, field validation failure, invalid normalized name, or empty patch",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Area not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "The updated name conflicts with another area",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public AdminAreaResponse updateArea(
            @Parameter(description = "ID of the area to update.", example = "10", required = true)
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Area fields to change.", required = true,
                    content = @Content(schema = @Schema(implementation = UpdateAreaRequest.class),
                            examples = @ExampleObject(value = "{\"name\":\"Riverside Garden\",\"description\":\"Quiet outdoor seating\"}"))
            )
            @Valid @RequestBody UpdateAreaRequest request
    ) {
        return adminAreaService.updateArea(id, request);
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(
            summary = "Deactivate a restaurant area",
            description = "Marks an area inactive so tables cannot be created in or moved into it. The area is retained rather than deleted. Access: ADMIN."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Area deactivated", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "The path ID is not a valid long integer",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Area not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public AdminAreaResponse deactivateArea(
            @Parameter(description = "ID of the area to deactivate.", example = "10", required = true)
            @PathVariable Long id) {
        return adminAreaService.deactivateArea(id);
    }
}
