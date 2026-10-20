package com.sagares.saga_res_api.order.service;

import com.sagares.saga_res_api.account.entity.Account;
import com.sagares.saga_res_api.account.repository.AccountRepository;
import com.sagares.saga_res_api.common.exception.BusinessException;
import com.sagares.saga_res_api.menu.entity.Dish;
import com.sagares.saga_res_api.menu.repository.DishRepository;
import com.sagares.saga_res_api.order.dto.*;
import com.sagares.saga_res_api.order.entity.FoodOrder;
import com.sagares.saga_res_api.order.entity.FoodOrderItem;
import com.sagares.saga_res_api.order.entity.FoodOrderStatus;
import com.sagares.saga_res_api.order.repository.FoodOrderItemRepository;
import com.sagares.saga_res_api.order.repository.FoodOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.sagares.saga_res_api.common.dto.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;


import com.sagares.saga_res_api.common.dto.PageResponse;
import com.sagares.saga_res_api.order.entity.FoodOrderStatus;
import org.springframework.security.access.prepost.PreAuthorize;

@Service
@RequiredArgsConstructor
public class OrderDishService {

    private final FoodOrderRepository foodOrderRepository;
    private final FoodOrderItemRepository foodOrderItemRepository;
    private final AccountRepository accountRepository;
    private final DishRepository dishRepository;

    @Transactional
    public OrderDishResponse create(long id, CreateOrderDishRequest request){

        if(!validatePhone(request.deliveryPhone())){
            throw new BusinessException(HttpStatus.BAD_REQUEST, "NUMBER NOT VALID", "Số điện thoại không đúng");
        }

        Account account = accountRepository.findById(id).orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "NOT FOUND ACCOUNT", "Không tìm thấy tài khoản"));

        FoodOrder foodOrder = new FoodOrder();

        foodOrder.setCustomer(account);
        foodOrder.setDeliveryAddress(request.deliveryAddress());
        foodOrder.setDeliveryPhone(request.deliveryPhone());

        FoodOrder foodOrderSave = foodOrderRepository.save(foodOrder);

        List<OrderDishItemResponse> orderDishItemResponseList = new ArrayList<>();
        int totalAmountPrice = 0;
        for(CreateOrderDishItemRequest item : request.createOrderDishItemRequestList()){

            Dish dish = dishRepository.findToOrderById(item.idDish()).orElseThrow(() -> new BusinessException(
                    HttpStatus.NOT_FOUND,
                    "DISH NOT FOUND",
                    "Không tìm thấy món ăn phù hợp"));

            FoodOrderItem foodOrderItem = new FoodOrderItem();

            foodOrderItem.setFoodOrder(foodOrderSave);
            foodOrderItem.setDish(dish);
            foodOrderItem.setQuantity(item.quantity());
            foodOrderItem.setUnitPrice(dish.getPrice());

            foodOrderItemRepository.save(foodOrderItem);

            int totalLine = item.quantity() * dish.getPrice();
            totalAmountPrice += totalLine;
            OrderDishItemResponse orderDishItemResponse = OrderDishItemResponse.toDTO(foodOrderItem);
            orderDishItemResponseList.add(orderDishItemResponse);

        }
        return toDTO(foodOrderSave, orderDishItemResponseList, totalAmountPrice);
    }

    @Transactional
    public OrderDetailDishResponse getDetail(Long idAccount, Long idRequest){

        FoodOrder foodOrder = foodOrderRepository.findOwnedDetail(idRequest, idAccount).orElseThrow(() -> new BusinessException(
                HttpStatus.NOT_FOUND,
                "FOOD ORDER NOT EXIXTS",
                "Không tìm thấy đơn đặt thức ăn"
        ));
        return OrderDetailDishResponse.toDTO(foodOrder);
    }

    @Transactional
    public OrderDetailDishResponse cancelledFoodOrder(Long idAccount, Long idRequest){
        FoodOrder foodOrder = foodOrderRepository.findOwnedDetail(idRequest, idAccount).orElseThrow(() -> new BusinessException(
                HttpStatus.NOT_FOUND,
                "FOOD ORDER NOT EXIXTS",
                "Không tìm thấy đơn đặt thức ăn"
        ));
        if(foodOrder.getStatus() != FoodOrderStatus.PLACED) {
            throw  new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "FOOD ORDER CANNOT CANCELLED",
                    "Trạng thái đơn đặt món hiện tại không thể hủy"
            );
        }
        foodOrder.setStatus(FoodOrderStatus.CANCELLED);
        foodOrder.setCancelledAt(Instant.now());
        FoodOrder foodOrderSave = foodOrderRepository.save(foodOrder);
        return OrderDetailDishResponse.toDTO(foodOrder);
    }


    @Transactional(readOnly = true)
    public PageResponse<OrderDetailDishResponse> listMine(
            Long customerId,
            FoodOrderStatus status,
            int page,
            int size
    ) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_PAGINATION",
                    "page phải >= 0; size phải từ 1 đến 100"
            );
        }

        var pageable = PageRequest.of(page, size);
        Page<Long> ids = status == null
                ? foodOrderRepository.findOwnedIds(customerId, pageable)
                : foodOrderRepository.findOwnedIdsByStatus(customerId, status, pageable);

        if (ids.isEmpty()) {
            // Giữ tổng số phần tử khi khách yêu cầu một trang vượt cuối danh sách.
            return PageResponse.from(new PageImpl<OrderDetailDishResponse>(
                    List.of(), pageable, ids.getTotalElements()
            ));
        }

        Map<Long, FoodOrder> byId = foodOrderRepository
                .findOwnedDetailsByIds(customerId, ids.getContent())
                .stream()
                .collect(Collectors.toMap(FoodOrder::getId, Function.identity()));

        return PageResponse.from(ids.map(id ->
                OrderDetailDishResponse.toDTO(byId.get(id))
        ));
    }


    private boolean validatePhone(String phone){
        String regex = "^\\d+$";
        return  phone.matches(regex);
    }

    private OrderDishResponse toDTO(FoodOrder foodOrder, List<OrderDishItemResponse> list, int totalAmountPrice){
        OrderDishResponse result = new OrderDishResponse(
                foodOrder.getId(),
                foodOrder.getStatus(),
                foodOrder.getCreatedAt(),
                totalAmountPrice,
                foodOrder.getDeliveryAddress(),
                foodOrder.getDeliveryPhone(),
                foodOrder.getPaymentMethod(),
                list
        );
        return result;
    }

}
