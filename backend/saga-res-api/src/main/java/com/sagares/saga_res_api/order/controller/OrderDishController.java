package com.sagares.saga_res_api.order.controller;

import com.sagares.saga_res_api.order.dto.CreateOrderDishRequest;
import com.sagares.saga_res_api.order.dto.OrderDetailDishResponse;
import com.sagares.saga_res_api.order.dto.OrderDishResponse;
import com.sagares.saga_res_api.order.service.OrderDishService;
import com.sagares.saga_res_api.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.sagares.saga_res_api.common.dto.PageResponse;
import com.sagares.saga_res_api.order.entity.FoodOrderStatus;

@RestController
@RequestMapping("/api/customer/orders")
@RequiredArgsConstructor
public class OrderDishController {

    private final OrderDishService orderDishService;

    @PostMapping
    public ResponseEntity<OrderDishResponse> create (@Valid @RequestBody CreateOrderDishRequest request, @AuthenticationPrincipal AuthenticatedUser authenticatedUser){
        OrderDishResponse data = orderDishService.create(authenticatedUser.accountId(), request);

        return ResponseEntity.status(HttpStatus.CREATED).body(data);
    }
    @GetMapping("/{id}")
    public ResponseEntity<OrderDetailDishResponse> getDetail (@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser authenticatedUser){
        OrderDetailDishResponse data = orderDishService.getDetail(authenticatedUser.accountId(), id);
        return ResponseEntity.ok(data);
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<OrderDetailDishResponse> cancelled (@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser authenticatedUser){
        OrderDetailDishResponse data = orderDishService.cancelledFoodOrder(authenticatedUser.accountId(), id);
        return ResponseEntity.ok(data);
    }

    @GetMapping

    public PageResponse<OrderDetailDishResponse> listMine(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(name = "status", required = false) FoodOrderStatus status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "4") int size
    ) {
        return orderDishService.listMine(user.accountId(), status, page, size);
    }
}
