package  com.sagares.saga_res_api.reservation.repository;


import com.sagares.saga_res_api.reservation.entity.Reservation;
import com.sagares.saga_res_api.reservation.entity.ReservationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long>, JpaSpecificationExecutor<Reservation> {


    @Override
    @EntityGraph(attributePaths = {
            "requestedArea",
            "customer",
            "table",
            "table.area"
    })
    Page<Reservation> findAll(
            Specification<Reservation> spec,
            Pageable pageable
    );

    @Query(value = """
        SELECT r.id
        FROM reservation r
        WHERE r.status IN ('PENDING', 'CONFIRMED', 'ARRIVED')
          AND r.end_time > CURRENT_TIMESTAMP
          AND EXTRACT(DOW FROM r.start_time AT TIME ZONE 'Asia/Ho_Chi_Minh') = :dayOfWeek
          AND (
              :closed = true
              OR CAST(r.start_time AT TIME ZONE 'Asia/Ho_Chi_Minh' AS time) < :openTime
              OR CAST(r.end_time AT TIME ZONE 'Asia/Ho_Chi_Minh' AS time) > :closeTime
              OR CAST(r.start_time AT TIME ZONE 'Asia/Ho_Chi_Minh' AS date)
                  <> CAST(r.end_time AT TIME ZONE 'Asia/Ho_Chi_Minh' AS date)
          )
        ORDER BY r.start_time, r.id
        """, nativeQuery = true)
    List<Long> findIdsOutsideOpeningHours(
            @Param("dayOfWeek") Short dayOfWeek,
            @Param("openTime") LocalTime openTime,
            @Param("closeTime") LocalTime closeTime,
            @Param("closed") boolean closed);

    @Query(value = """
        SELECT r.*
        FROM reservation r
        WHERE r.status IN ('PENDING','CONFIRMED','ARRIVED','COMPLETED')
          AND r.occupied_range && tstzrange(:dayStart, :dayEnd, '[)')
        """, nativeQuery = true)
    List<Reservation> findTableOccupancyInRange(
        @Param("dayStart") Instant dayStart,
        @Param("dayEnd") Instant dayEnd
    );
    // Chỉ PENDING và CONFIRMED được tính vào giới hạn đơn đang hoạt động.
    @Query(value = """
        SELECT COUNT(*)
        FROM reservation r
        WHERE r.customer_id = :customerId
          AND r.status IN ('PENDING', 'CONFIRMED')
        """, nativeQuery = true)
    long countActiveByCustomer(
            @Param("customerId") Long customerId
    );

    // Kiểm tra thời gian chiếm bàn, gồm cả thời gian dọn bàn.
// COMPLETED vẫn có thể chặn bàn nếu buffer chưa kết thúc.
    @Query(value = """
        SELECT EXISTS (
            SELECT 1
            FROM reservation r
            WHERE r.table_id = :tableId
              AND r.status IN (
                  'PENDING', 'CONFIRMED', 'ARRIVED', 'COMPLETED'
              )
              AND r.occupied_range
                  && tstzrange(:startTime, :occupiedEnd, '[)')
        )
        """, nativeQuery = true)
    boolean existsTableOverlap(
            @Param("tableId") Long tableId,
            @Param("startTime") Instant startTime,
            @Param("occupiedEnd") Instant occupiedEnd
    );

    // Kiểm tra khách đã có đơn trùng khoảng thời gian đang muốn đặt chưa.
    @Query(value = """
        SELECT EXISTS (
            SELECT 1
            FROM reservation r
            WHERE r.customer_id = :customerId
              AND r.status IN ('PENDING', 'CONFIRMED', 'ARRIVED')
              AND r.reservation_range
                  && tstzrange(:startTime, :endTime, '[)')
        )
        """, nativeQuery = true)
    boolean existsCustomerOverlap(
            @Param("customerId") Long customerId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime
    );

    Optional<Reservation> findByCode(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reservation r where r.id = :id")
    Optional<Reservation> findByIdForUpdate(@Param("id") Long id);

    Optional<Reservation> findByIdAndCustomer_Id(
            Long id,
            Long customerId
    );


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select r
        from Reservation r
        where r.id = :id
          and r.customer.id = :customerId
        """)
    Optional<Reservation> findOwnedByIdForUpdate(
            @Param("id") Long id,
            @Param("customerId") Long customerId
    );

    @Query(value = """
        SELECT EXISTS (
            SELECT 1
            FROM reservation r
            WHERE r.id <> :reservationId
              AND r.customer_id = :customerId
              AND r.status IN ('PENDING', 'CONFIRMED', 'ARRIVED')
              AND r.reservation_range
                  && tstzrange(:startTime, :endTime, '[)')
        )
        """, nativeQuery = true)
    boolean existsCustomerOverlapExcludingReservation(
            @Param("reservationId") Long reservationId,
            @Param("customerId") Long customerId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime
    );

    @Query(value = """
        SELECT EXISTS (
            SELECT 1
            FROM reservation r
            WHERE r.id <> :reservationId
              AND r.table_id = :tableId
              AND r.status IN (
                  'PENDING', 'CONFIRMED', 'ARRIVED', 'COMPLETED'
              )
              AND r.occupied_range
                  && tstzrange(:startTime, :occupiedEnd, '[)')
        )
        """, nativeQuery = true)
    boolean existsTableOverlapExcludingReservation(
            @Param("reservationId") Long reservationId,
            @Param("tableId") Long tableId,
            @Param("startTime") Instant startTime,
            @Param("occupiedEnd") Instant occupiedEnd
    );

    @EntityGraph(attributePaths = {
            "customer",
            "table",
            "table.area",
            "requestedArea"
    })
    @Query("""
        select r
        from Reservation r
        where r.id = :id
        """)
    Optional<Reservation> findStaffDetailById(
            @Param("id") Long id
    );

    interface DurationConflictRow {

        Long getId();

        String getCode();

        Long getTableId();

        Instant getStartTime();

        Instant getEndTime();

        Integer getBufferMinutes();
    }

    @Query(value = """
    SELECT
        r.id AS "id",
        r.code AS "code",
        r.table_id AS "tableId",
        r.start_time AS "startTime",
        r.end_time AS "endTime",
        CAST(r.buffer_minutes AS integer) AS "bufferMinutes"
    FROM reservation r
    WHERE r.id <> :reservationId
      AND (
          (
              r.table_id = :tableId
              AND r.status IN (
                  'PENDING', 'CONFIRMED', 'ARRIVED', 'COMPLETED'
              )
              AND r.occupied_range
                  && tstzrange(:startTime, :occupiedEnd, '[)')
          )
          OR
          (
              r.customer_id = :customerId
              AND r.status IN ('PENDING', 'CONFIRMED', 'ARRIVED')
              AND r.reservation_range
                  && tstzrange(:startTime, :endTime, '[)')
          )
      )
    ORDER BY r.start_time, r.id
    """, nativeQuery = true)
    List<DurationConflictRow> findDurationConflicts(
            @Param("reservationId") Long reservationId,
            @Param("tableId") Long tableId,
            @Param("customerId") Long customerId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime,
            @Param("occupiedEnd") Instant occupiedEnd
    );
    @Query("""
    select r.id
    from Reservation r
    where r.status = :status
      and r.startTime <= :threshold
    order by r.startTime, r.id
    """)
    List<Long> findIdsByStatusAndStartTimeAtOrBefore(
            @Param("status") ReservationStatus status,
            @Param("threshold") Instant threshold,
            Pageable pageable);
}
