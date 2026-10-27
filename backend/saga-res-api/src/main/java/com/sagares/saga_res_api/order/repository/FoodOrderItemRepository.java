package  com.sagares.saga_res_api.order.repository;


import com.sagares.saga_res_api.order.entity.FoodOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FoodOrderItemRepository extends JpaRepository<FoodOrderItem, Long> {

    List<FoodOrderItem> findByFoodOrderId(Long foodOrderId);

    interface StaffOrderItemRow {

        Long getOrderId();

        Integer getQuantity();

        Integer getUnitPrice();
    }

    @Query("""
    select
        i.foodOrder.id as orderId,
        i.quantity as quantity,
        i.unitPrice as unitPrice
    from FoodOrderItem i
    where i.foodOrder.id in :orderIds
    """)
    List<StaffOrderItemRow> findStaffItemRowsByOrderIds(
            @Param("orderIds") List<Long> orderIds
    );
}
