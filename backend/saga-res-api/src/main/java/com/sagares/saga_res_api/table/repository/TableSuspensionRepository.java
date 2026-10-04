package com.sagares.saga_res_api.table.repository;


import com.sagares.saga_res_api.table.entity.TableSuspension;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface TableSuspensionRepository extends JpaRepository<TableSuspension, Long> {

    @Query("""
        select s from TableSuspension s
        where s.table.id = :tableId
          and s.startTime < :occupiedEnd
          and s.endTime > :occupiedStart
        """)
    List<TableSuspension> findOverlapping(
        @Param("tableId") Long tableId,
        @Param("occupiedStart") Instant occupiedStart,
        @Param("occupiedEnd") Instant occupiedEnd
    );

    @Query("""
        select s from TableSuspension s
        where s.startTime < :rangeEnd
          and s.endTime > :rangeStart
        """)
    List<TableSuspension> findAllOverlapping(
        @Param("rangeStart") Instant rangeStart,
        @Param("rangeEnd") Instant rangeEnd
    );
}
