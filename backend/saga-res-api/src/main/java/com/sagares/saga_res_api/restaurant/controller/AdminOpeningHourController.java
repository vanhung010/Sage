package com.sagares.saga_res_api.restaurant.controller;

import com.sagares.saga_res_api.restaurant.dto.admin.AdminOpeningHourResponse;
import com.sagares.saga_res_api.restaurant.dto.admin.CreateOpeningHourRequest;
import com.sagares.saga_res_api.restaurant.dto.admin.UpdateOpeningHourRequest;
import com.sagares.saga_res_api.restaurant.service.OpeningHourService;
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
@RequestMapping("/api/admin/opening-hours")
@RequiredArgsConstructor
@Tag(
        name = "Admin Opening Hours",
        description = "Manage weekly opening-hour configuration. Despite the admin URL, the active SecurityConfig currently permits anonymous access."
)
public class AdminOpeningHourController {

    private final OpeningHourService openingHourService;

    @GetMapping
    @Operation(
            summary = "List opening-hour configuration",
            description = "Returns every configured weekday entry sorted from Monday through Sunday. Access: anonymous under the active SecurityConfig."
    )
    @ApiResponse(responseCode = "200", description = "Opening-hour configuration returned", useReturnTypeSchema = true)
    public List<AdminOpeningHourResponse> getOpeningHours() {
        return openingHourService.listAdminOpeningHours();
    }

    @PostMapping
    @Operation(
            summary = "Create a weekday opening-hour entry",
            description = """
                    Adds opening and closing times for a weekday that is not already configured. The server derives the
                    numeric weekday key from dayOfWeek and creates the entry as open. Access: anonymous under the active SecurityConfig.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Opening-hour entry created", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "Malformed JSON, invalid weekday, missing time, or closeTime is not later than openTime",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "The weekday already has an opening-hour entry",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AdminOpeningHourResponse> createOpeningHour(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Weekday and local opening interval.", required = true,
                    content = @Content(schema = @Schema(implementation = CreateOpeningHourRequest.class),
                            examples = @ExampleObject(value = "{\"dayOfWeek\":\"MONDAY\",\"openTime\":\"10:00:00\",\"closeTime\":\"22:00:00\"}"))
            )
            @Valid @RequestBody CreateOpeningHourRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(openingHourService.createOpeningHour(request));
    }

    @PatchMapping("/{id}")
    @Operation(
            summary = "Update a weekday opening-hour entry",
            description = """
                    Partially changes the local opening interval for an existing weekday. The optional dayOfWeek value
                    may echo the stored weekday but cannot change it. Existing reservations are not cancelled; the
                    response lists active future reservation IDs that fall outside the new interval. Access: anonymous
                    under the active SecurityConfig.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opening-hour entry updated, with any impacted reservation IDs", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "Invalid path ID, malformed JSON, invalid or changed weekday, empty patch, missing resulting time, or invalid time order",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Opening-hour entry not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public AdminOpeningHourResponse updateOpeningHour(
            @Parameter(
                    description = "Weekday key: 0 for Sunday and 1 through 6 for Monday through Saturday.",
                    example = "1",
                    required = true,
                    schema = @Schema(minimum = "0", maximum = "6")
            )
            @PathVariable Short id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Opening-hour fields to change.", required = true,
                    content = @Content(schema = @Schema(implementation = UpdateOpeningHourRequest.class),
                            examples = @ExampleObject(value = "{\"dayOfWeek\":\"MONDAY\",\"openTime\":\"09:30:00\",\"closeTime\":\"22:30:00\"}"))
            )
            @Valid @RequestBody UpdateOpeningHourRequest request) {
        return openingHourService.updateOpeningHour(id, request);
    }
}
