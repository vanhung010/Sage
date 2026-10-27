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
import java.time.Instant;
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
    interface MoveTableOptionRow {

        Long getId();

        String getCode();

        Long getAreaId();

        String getAreaName();

        int getMinCapacity();

        int getMaxCapacity();

        boolean getActive();

        Instant getBusyStart();

        Instant getBusyEnd();

        Instant getSuspendedStart();

        Instant getSuspendedEnd();

        String getSuspensionReason();
    }

    @Query(value = """
    SELECT
        t.id AS "id",
        t.code AS "code",
        a.id AS "areaId",
        a.name AS "areaName",
        CAST(t.min_capacity AS integer) AS "minCapacity",
        CAST(t.max_capacity AS integer) AS "maxCapacity",
        t.is_active AS "active",
        busy.start_time AS "busyStart",
        busy.end_time AS "busyEnd",
        suspension.start_time AS "suspendedStart",
        suspension.end_time AS "suspendedEnd",
        suspension.reason AS "suspensionReason"
    FROM restaurant_table t
    JOIN area a ON a.id = t.area_id

    LEFT JOIN LATERAL (
        SELECT
            lower(r.occupied_range) AS start_time,
            upper(r.occupied_range) AS end_time
        FROM reservation r
        WHERE r.table_id = t.id
          AND r.id <> :reservationId
          AND r.status IN (
              'PENDING', 'CONFIRMED', 'ARRIVED', 'COMPLETED'
          )
          AND r.occupied_range
              && tstzrange(:occupiedStart, :occupiedEnd, '[)')
        ORDER BY lower(r.occupied_range), r.id
        LIMIT 1
    ) busy ON true

    LEFT JOIN LATERAL (
        SELECT
            s.start_time,
            s.end_time,
            s.reason
        FROM table_suspension s
        WHERE s.table_id = t.id
          AND s.start_time < :occupiedEnd
          AND s.end_time > :occupiedStart
        ORDER BY s.start_time, s.id
        LIMIT 1
    ) suspension ON true

    WHERE t.id <> :currentTableId
      AND (
          CAST(:areaId AS bigint) IS NULL
          OR t.area_id = CAST(:areaId AS bigint)
      )

    ORDER BY
        CASE
            WHEN t.is_active = true
             AND t.min_capacity <= :partySize
             AND t.max_capacity >= :partySize
             AND busy.start_time IS NULL
             AND suspension.start_time IS NULL
            THEN 0
            ELSE 1
        END,
        CASE
            WHEN t.area_id = CAST(:requestedAreaId AS bigint)
            THEN 0
            ELSE 1
        END,
        t.max_capacity,
        t.code,
        t.id
    """, nativeQuery = true)
    List<MoveTableOptionRow> findMoveTableOptions(
            @Param("reservationId") Long reservationId,
            @Param("currentTableId") Long currentTableId,
            @Param("partySize") int partySize,
            @Param("areaId") Long areaId,
            @Param("requestedAreaId") Long requestedAreaId,
            @Param("occupiedStart") Instant occupiedStart,
            @Param("occupiedEnd") Instant occupiedEnd
    );
    // Lấy cả bàn hoạt động và ngừng hoạt động.
// JOIN FETCH tải khu vực cùng bàn, tránh truy vấn riêng từng khu vực.
    @Query("""
        select t
        from RestaurantTable t
        join fetch t.area a
        where (:areaId is null or a.id = :areaId)
        order by a.name asc, t.code asc, t.id asc
        """)
    List<RestaurantTable> findForTimeline(
            @Param("areaId") Long areaId
    );
}
