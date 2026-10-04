package  com.sagares.saga_res_api.menu.repository;


import com.sagares.saga_res_api.menu.entity.Dish;
import com.sagares.saga_res_api.menu.entity.DishStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DishRepository extends JpaRepository<Dish, Long> {

    List<Dish> findByCategoryIdAndStatusOrderByDisplayOrderAscNameAsc(
        Long categoryId,
        DishStatus status
    );

    List<Dish> findByStatusOrderByDisplayOrderAscNameAsc(DishStatus status);
}
