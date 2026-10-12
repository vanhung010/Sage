package com.sagares.saga_res_api.table.repository;


import com.sagares.saga_res_api.table.entity.Area;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AreaRepository extends JpaRepository<Area, Long> {

    List<Area> findByActiveTrueOrderByNameAsc();

    List<Area> findAllByOrderByNameAsc();

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    Optional<Area> findByName(String name);
}
