package com.sagares.saga_res_api.table.repository;


import com.sagares.saga_res_api.table.entity.RestaurantTable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RestaurantTableRepository extends JpaRepository<RestaurantTable, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from RestaurantTable t where t.id = :id")
    Optional<RestaurantTable> findByIdForUpdate(@Param("id") Long id);

    List<RestaurantTable> findByActiveTrueOrderByMaxCapacityAscCodeAsc();

    List<RestaurantTable> findByAreaIdAndActiveTrueOrderByMaxCapacityAscCodeAsc(Long areaId);

    @EntityGraph(attributePaths = "area")
    List<RestaurantTable> findAllByOrderByArea_NameAscCodeAsc();

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, Long id);

    // Ưu tiên bàn có sức chứa nhỏ nhất nhưng vẫn phù hợp.
// code giúp thứ tự chọn ổn định khi nhiều bàn có cùng sức chứa.
//
// Chỉ lấy ID trước. Khi chuẩn bị đặt, service sẽ khóa và đọc lại bàn.
    @Query("""
        select t.id
        from RestaurantTable t
        where t.active = true
          and t.minCapacity <= :partySize
          and t.maxCapacity >= :partySize
          and (:areaId is null or t.area.id = :areaId)
        order by t.maxCapacity asc, t.code asc
        """)
    List<Long> findCandidateIds(
            @Param("partySize") short partySize,
            @Param("areaId") Long areaId
    );

}
