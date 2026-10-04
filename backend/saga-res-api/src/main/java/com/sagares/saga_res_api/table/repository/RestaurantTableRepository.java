package com.sagares.saga_res_api.table.repository;


import com.sagares.saga_res_api.table.entity.RestaurantTable;
import jakarta.persistence.LockModeType;
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
}
