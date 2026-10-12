package  com.sagares.saga_res_api.reservation.repository;


import com.sagares.saga_res_api.reservation.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

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

//    @Query("""
//        select count(r)
//        from Reservation r
//        where r.customer.id = :customerId
//          and r.status in (
//              com.sagares.reservation.entity.ReservationStatus.PENDING,
//              com.sagares.reservation.entity.ReservationStatus.CONFIRMED
//          )
//        """)
//    long countActiveByCustomer(@Param("customerId") Long customerId);

    Optional<Reservation> findByCode(String code);
}
