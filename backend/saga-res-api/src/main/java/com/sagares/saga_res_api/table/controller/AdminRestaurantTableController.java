package com.sagares.saga_res_api.table.controller;

import com.sagares.saga_res_api.table.dto.admin.AdminRestaurantTableResponse;
import com.sagares.saga_res_api.table.dto.admin.CreateRestaurantTableRequest;
import com.sagares.saga_res_api.table.dto.admin.UpdateRestaurantTableRequest;
import com.sagares.saga_res_api.table.service.AdminRestaurantTableService;
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
@RequestMapping("/api/admin/tables")
@RequiredArgsConstructor
@Tag(
        name = "Admin Restaurant Tables",
        description = "Manage physical restaurant tables and capacity ranges. Despite the admin URL, the active SecurityConfig currently permits anonymous access."
)
public class AdminRestaurantTableController {

    private final AdminRestaurantTableService adminRestaurantTableService;

    @GetMapping
    @Operation(
            summary = "List restaurant tables",
            description = "Returns all tables, including inactive entries, ordered by area name and then table code. Access: anonymous under the active SecurityConfig."
    )
    @ApiResponse(responseCode = "200", description = "Restaurant tables returned", useReturnTypeSchema = true)
    public List<AdminRestaurantTableResponse> getTables() {
        return adminRestaurantTableService.listTables();
    }

    @PostMapping
    @Operation(
            summary = "Create a restaurant table",
            description = """
                    Creates an active table in an existing active area. Its minimum and maximum capacities define which
                    party sizes fit the table, and maximum capacity cannot be below minimum capacity. Access: anonymous
                    under the active SecurityConfig.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Restaurant table created", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "Malformed JSON, field validation failure, invalid normalized code, or invalid capacity range",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "The referenced area does not exist",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "The table code already exists or the referenced area is inactive",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AdminRestaurantTableResponse> createTable(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "New table values.", required = true,
                    content = @Content(schema = @Schema(implementation = CreateRestaurantTableRequest.class),
                            examples = @ExampleObject(value = "{\"code\":\"T01\",\"areaId\":10,\"minCapacity\":2,\"maxCapacity\":4}"))
            )
            @Valid @RequestBody CreateRestaurantTableRequest request
    ) {
        AdminRestaurantTableResponse response = adminRestaurantTableService.createTable(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}")
    @Operation(
            summary = "Update a restaurant table",
            description = """
                    Partially changes a table's code, area, or capacity range while holding the table row lock. A newly
                    selected area must exist and be active, and the resulting maximum capacity cannot be below the minimum.
                    Access: anonymous under the active SecurityConfig.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Restaurant table updated", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "Invalid path ID, malformed JSON, field validation failure, invalid normalized code, invalid capacity range, or empty patch",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "The table or referenced area does not exist",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "The updated code conflicts with another table or the new area is inactive",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public AdminRestaurantTableResponse updateTable(
            @Parameter(description = "ID of the table to update.", example = "100", required = true)
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Table fields to change.", required = true,
                    content = @Content(schema = @Schema(implementation = UpdateRestaurantTableRequest.class),
                            examples = @ExampleObject(value = "{\"code\":\"T02\",\"areaId\":11,\"minCapacity\":2,\"maxCapacity\":6}"))
            )
            @Valid @RequestBody UpdateRestaurantTableRequest request
    ) {
        return adminRestaurantTableService.updateTable(id, request);
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(
            summary = "Deactivate a restaurant table",
            description = "Marks a table inactive while retaining its record. The table row is locked during the update. Access: anonymous under the active SecurityConfig."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Restaurant table deactivated", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "The path ID is not a valid long integer",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Restaurant table not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public AdminRestaurantTableResponse deactivateTable(
            @Parameter(description = "ID of the table to deactivate.", example = "100", required = true)
            @PathVariable Long id) {
        return adminRestaurantTableService.deactivateTable(id);
    }
}
