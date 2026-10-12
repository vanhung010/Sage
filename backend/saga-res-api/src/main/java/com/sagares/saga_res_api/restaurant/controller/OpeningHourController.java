package com.sagares.saga_res_api.restaurant.controller;

import com.sagares.saga_res_api.restaurant.dto.OpeningHourResponse;
import com.sagares.saga_res_api.restaurant.service.OpeningHourService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/opening-hours")
@RequiredArgsConstructor
@Tag(name = "Opening Hours", description = "Public weekly restaurant opening-hour information.")
public class OpeningHourController {

    private final OpeningHourService openingHourService;

    @GetMapping
    @Operation(
            summary = "Get public opening hours",
            description = """
                    Returns the configured weekly opening-hour entries sorted from Monday through Sunday. Each entry
                    identifies the weekday, its local opening and closing times, and whether that weekday is closed.
                    Access: anonymous.
                    """
    )
    @ApiResponse(responseCode = "200", description = "Weekly opening hours returned", useReturnTypeSchema = true)
    public List<OpeningHourResponse> getOpeningHours() {
        return openingHourService.getPublicOpeningHours();
    }
}
