package com.sagares.saga_res_api.order.service;

import com.sagares.saga_res_api.common.dto.PageResponse;
import com.sagares.saga_res_api.common.exception.BusinessException;
import com.sagares.saga_res_api.order.dto.StaffOrderDetailResponse;
import com.sagares.saga_res_api.order.dto.StaffOrderSummaryResponse;
import com.sagares.saga_res_api.order.entity.FoodOrder;
import com.sagares.saga_res_api.order.entity.FoodOrderStatus;
import com.sagares.saga_res_api.order.repository.FoodOrderItemRepository;
import com.sagares.saga_res_api.order.repository.FoodOrderRepository;
import com.sagares.saga_res_api.order.specification.FoodOrderSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
public class StaffOrderService {

    private final FoodOrderRepository foodOrderRepository;
    private final FoodOrderItemRepository foodOrderItemRepository;

    @Transactional(readOnly = true)
    public PageResponse<StaffOrderSummaryResponse> list(
            String search,
            LocalDate date,
            FoodOrderStatus status,
            int page,
            int size
    ) {
        validateRequest(search, page, size);

        Specification<FoodOrder> specification =
                FoodOrderSpecifications.createdOn(date)
                        .and(FoodOrderSpecifications.hasStatus(status))
                        .and(FoodOrderSpecifications.matchesSearch(search));

        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Direction.DESC,
                        "createdAt",
                        "id"
                )
        );

        // Chỉ phân trang FoodOrder và tải customer.
        Page<FoodOrder> orders = foodOrderRepository.findAll(
                specification,
                pageable
        );

        Map<Long, Totals> totalsByOrder = new HashMap<>();

        if (!orders.isEmpty()) {
            List<Long> orderIds = orders.getContent()
                    .stream()
                    .map(FoodOrder::getId)
                    .toList();

            // Một truy vấn chung cho items của cả trang.
            var itemRows = foodOrderItemRepository
                    .findStaffItemRowsByOrderIds(orderIds);

            for (var item : itemRows) {
                long quantity = item.getQuantity().longValue();

                // Chuyển sang long trước phép nhân.
                long lineTotal =
                        item.getUnitPrice().longValue() * quantity;

                totalsByOrder.merge(
                        item.getOrderId(),
                        new Totals(quantity, lineTotal),
                        Totals::plus
                );
            }
        }

        // map trên Page giữ nguyên totalElements và totalPages,
        // kể cả khi client yêu cầu trang vượt cuối danh sách.
        return PageResponse.from(
                orders.map(order -> toResponse(
                        order,
                        totalsByOrder.getOrDefault(
                                order.getId(),
                                Totals.ZERO
                        )
                ))
        );
    }


    @Transactional(readOnly = true)
    public StaffOrderDetailResponse getDetail(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_ORDER_ID",
                    "Mã đơn đặt món phải là số nguyên dương"
            );
        }

        FoodOrder foodOrder = foodOrderRepository
                .findDetailById(id)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "FOOD_ORDER_NOT_FOUND",
                        "Không tìm thấy đơn đặt món"
                ));

        return StaffOrderDetailResponse.toDTO(foodOrder);
    }

    private void validateRequest(
            String search,
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

        if (search != null && search.strip().length() > 255) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_SEARCH",
                    "Từ khóa tìm kiếm không được vượt quá 255 ký tự"
            );
        }
    }

    private StaffOrderSummaryResponse toResponse(
            FoodOrder order,
            Totals totals
    ) {
        var customer = order.getCustomer();

        return new StaffOrderSummaryResponse(
                order.getId(),
                customer.getId(),
                customer.getFullName(),
                customer.getPhone(),
                order.getDeliveryPhone(),
                order.getCreatedAt(),
                order.getStatus(),
                totals.quantity(),
                totals.amount(),
                order.getPaymentMethod(),
                order.getCancelledAt()
        );
    }

    private record Totals(
            long quantity,
            long amount
    ) {
        private static final Totals ZERO = new Totals(0, 0);

        private Totals plus(Totals other) {
            return new Totals(
                    quantity + other.quantity,
                    amount + other.amount
            );
        }
    }
}