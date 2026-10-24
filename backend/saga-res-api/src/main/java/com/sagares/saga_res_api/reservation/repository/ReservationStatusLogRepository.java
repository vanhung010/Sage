package  com.sagares.saga_res_api.reservation.repository;


import com.sagares.saga_res_api.reservation.entity.ReservationStatusLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReservationStatusLogRepository extends JpaRepository<ReservationStatusLog, Long> {

    List<ReservationStatusLog> findByReservationIdOrderByChangedAtAsc(Long reservationId);

    @EntityGraph(attributePaths = "changedBy")
    @Query("""
        select log
        from ReservationStatusLog log
        where log.reservation.id = :reservationId
        order by log.changedAt desc, log.id desc
        """)
    List<ReservationStatusLog> findStaffDetailLogs(
            @Param("reservationId") Long reservationId
    );
}
