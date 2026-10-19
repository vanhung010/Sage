package com.sagares.saga_res_api.order.service;

import com.sagares.saga_res_api.account.entity.Account;
import com.sagares.saga_res_api.account.repository.AccountRepository;
import com.sagares.saga_res_api.common.exception.BusinessException;
import com.sagares.saga_res_api.menu.entity.Dish;
import com.sagares.saga_res_api.menu.repository.DishRepository;
import com.sagares.saga_res_api.order.dto.*;
import com.sagares.saga_res_api.order.entity.FoodOrder;
import com.sagares.saga_res_api.order.entity.FoodOrderItem;
import com.sagares.saga_res_api.order.repository.FoodOrderItemRepository;
import com.sagares.saga_res_api.order.repository.FoodOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

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
            OrderDishItemResponse orderDishItemResponse = new OrderDishItemResponse(dish.getName(), item.quantity(), dish.getPrice(), totalLine);
            orderDishItemResponseList.add(orderDishItemResponse);

        }
        return toDTO(foodOrderSave, orderDishItemResponseList, totalAmountPrice);
    }

    @Transactional(readOnly = true)
    public OrderDetailDishResponse getDetail(Long idAccount, Long idRequest){

        FoodOrder foodOrder = foodOrderRepository.findOwnedDetail(idRequest, idAccount).orElseThrow(() -> new BusinessException(
                HttpStatus.NOT_FOUND,
                "FOOD ORDER NOT EXIXTS",
                "Không tìm thấy đơn đặt thức ăn"
        ));
        return OrderDetailDishResponse.toDTO(foodOrder);
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
