package  com.sagares.saga_res_api.reservation.repository;


import com.sagares.saga_res_api.reservation.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

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
