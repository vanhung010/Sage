package  com.sagares.saga_res_api.order.repository;


import com.sagares.saga_res_api.order.entity.FoodOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FoodOrderRepository extends JpaRepository<FoodOrder, Long> {

    Page<FoodOrder> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<FoodOrder> findByCustomerIdOrderByCreatedAtDesc(Long customerId, Pageable pageable);

    @Query("""
        select distinct o
        from FoodOrder o
        left join fetch o.customer
        left join fetch o.items i
        left join fetch i.dish
        where o.id = :id
        """)
    Optional<FoodOrder> findDetailById(@Param("id") Long id);

    @Query("""
        select distinct o
        from FoodOrder o
        left join fetch o.items i
        left join fetch i.dish
        where o.id = :id
          and o.customer.id = :customerId
        """)
    Optional<FoodOrder> findOwnedDetail(
        @Param("id") Long id,
        @Param("customerId") Long customerId
    );
}
