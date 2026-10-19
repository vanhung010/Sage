package com.sagares.saga_res_api.order.controller;

import com.sagares.saga_res_api.order.dto.CreateOrderDishRequest;
import com.sagares.saga_res_api.order.dto.OrderDishResponse;
import com.sagares.saga_res_api.order.service.OrderDishService;
import com.sagares.saga_res_api.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderDishController {

    private final OrderDishService orderDishService;

    @PostMapping
    public ResponseEntity<OrderDishResponse> create (@RequestParam CreateOrderDishRequest request, @AuthenticationPrincipal AuthenticatedUser authenticatedUser){
        OrderDishResponse data = orderDishService.create(authenticatedUser.accountId(), request);

        return ResponseEntity.status(HttpStatus.CREATED).body(data);
    }
}
