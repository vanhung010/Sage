package  com.sagares.saga_res_api.order.repository;


import com.sagares.saga_res_api.order.entity.FoodOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FoodOrderItemRepository extends JpaRepository<FoodOrderItem, Long> {

    List<FoodOrderItem> findByFoodOrderId(Long foodOrderId);
}
