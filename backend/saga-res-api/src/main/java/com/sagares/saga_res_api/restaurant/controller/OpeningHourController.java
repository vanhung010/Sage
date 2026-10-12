package com.sagares.saga_res_api.restaurant.controller;

import com.sagares.saga_res_api.restaurant.dto.OpeningHourResponse;
import com.sagares.saga_res_api.restaurant.service.OpeningHourService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/opening-hours")
@RequiredArgsConstructor
public class OpeningHourController {

    private final OpeningHourService openingHourService;

    @GetMapping
    public List<OpeningHourResponse> getOpeningHours() {
        return openingHourService.getPublicOpeningHours();
    }
}
