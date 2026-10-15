package  com.sagares.saga_res_api.menu.repository;


import com.sagares.saga_res_api.menu.entity.Dish;
import com.sagares.saga_res_api.menu.entity.DishStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DishRepository extends JpaRepository<Dish, Long> {

    List<Dish> findByCategoryIdAndStatusOrderByDisplayOrderAscNameAsc(
        Long categoryId,
        DishStatus status
    );

    List<Dish> findByStatusOrderByDisplayOrderAscNameAsc(DishStatus status);

    boolean existsByTags_Id(Long tagId);

    @Query(value = """
            select d.id
            from Dish d
            where (:categoryId is null or d.category.id = :categoryId)
              and (:status is null or d.status = :status)
            """,
            countQuery = """
            select count(d)
            from Dish d
            where (:categoryId is null or d.category.id = :categoryId)
              and (:status is null or d.status = :status)
            """)
    Page<Long> findAdminDishIds(
            @Param("categoryId") Long categoryId,
            @Param("status") DishStatus status,
            Pageable pageable
    );

    @Query("""
            select distinct d
            from Dish d
            join fetch d.category
            left join fetch d.tags
            where d.id in :ids
            """)
    List<Dish> findAllAdminDetailsByIdIn(@Param("ids") Collection<Long> ids);

    @Query("""
            select distinct d
            from Dish d
            join fetch d.category
            left join fetch d.tags
            where d.id = :id
            """)
    Optional<Dish> findAdminDetailById(@Param("id") Long id);

    @Query(value = """
            select d.id
            from Dish d
            where d.status <> :hiddenStatus
              and (:searchPattern is null or lower(d.name) like :searchPattern escape '\\')
              and (:categoryId is null or d.category.id = :categoryId)
              and (:minPrice is null or d.price >= :minPrice)
              and (:maxPrice is null or d.price <= :maxPrice)
              and (:tagId is null or exists (
                  select t.id from Dish tagged join tagged.tags t
                  where tagged.id = d.id and t.id = :tagId
              ))
            """,
            countQuery = """
            select count(d)
            from Dish d
            where d.status <> :hiddenStatus
              and (:searchPattern is null or lower(d.name) like :searchPattern escape '\\')
              and (:categoryId is null or d.category.id = :categoryId)
              and (:minPrice is null or d.price >= :minPrice)
              and (:maxPrice is null or d.price <= :maxPrice)
              and (:tagId is null or exists (
                  select t.id from Dish tagged join tagged.tags t
                  where tagged.id = d.id and t.id = :tagId
              ))
            """)
    Page<Long> findPublicDishIds(
            @Param("hiddenStatus") DishStatus hiddenStatus,
            @Param("searchPattern") String searchPattern,
            @Param("categoryId") Long categoryId,
            @Param("minPrice") Integer minPrice,
            @Param("maxPrice") Integer maxPrice,
            @Param("tagId") Long tagId,
            Pageable pageable
    );

    @Query("""
            select distinct d
            from Dish d
            join fetch d.category
            left join fetch d.tags
            where d.id in :ids and d.status <> :hiddenStatus
            """)
    List<Dish> findAllPublicDetailsByIdIn(
            @Param("ids") Collection<Long> ids,
            @Param("hiddenStatus") DishStatus hiddenStatus
    );

    @Query("""
            select distinct d
            from Dish d
            join fetch d.category
            left join fetch d.tags
            where d.id = :id and d.status <> :hiddenStatus
            """)
    Optional<Dish> findPublicDetailById(
            @Param("id") Long id,
            @Param("hiddenStatus") DishStatus hiddenStatus
    );
}
