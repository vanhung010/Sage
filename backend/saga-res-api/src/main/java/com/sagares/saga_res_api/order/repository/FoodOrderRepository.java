package  com.sagares.saga_res_api.order.repository;


import com.sagares.saga_res_api.order.entity.FoodOrder;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import com.sagares.saga_res_api.order.entity.FoodOrderStatus;
import java.util.List;

import java.util.Optional;

public interface FoodOrderRepository extends JpaRepository<FoodOrder, Long>, JpaSpecificationExecutor<FoodOrder> {

    Page<FoodOrder> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<FoodOrder> findByCustomerIdOrderByCreatedAtDesc(Long customerId, Pageable pageable);


    @Query("""
    select o.id from FoodOrder o
    where o.customer.id = :customerId
    order by o.createdAt desc, o.id desc
    """)
    Page<Long> findOwnedIds(
            @Param("customerId") Long customerId,
            Pageable pageable
    );

    @Query("""
    select o.id from FoodOrder o
    where o.customer.id = :customerId
      and o.status = :status
    order by o.createdAt desc, o.id desc
    """)
    Page<Long> findOwnedIdsByStatus(
            @Param("customerId") Long customerId,
            @Param("status") FoodOrderStatus status,
            Pageable pageable
    );

    @Query("""
    select distinct o from FoodOrder o
    left join fetch o.items i
    left join fetch i.dish
    where o.customer.id = :customerId
      and o.id in :ids
    """)
    List<FoodOrder> findOwnedDetailsByIds(
            @Param("customerId") Long customerId,
            @Param("ids") List<Long> ids
    );

    @Query("""
        select distinct o
        from FoodOrder o
        left join fetch o.customer
        left join fetch o.items i
        left join fetch i.dish
        where o.id = :id
        """)
    Optional<FoodOrder> findDetailById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
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


    @Override
    @EntityGraph(attributePaths = "customer")
    Page<FoodOrder> findAll(
            Specification<FoodOrder> specification,
            Pageable pageable
    );
}
