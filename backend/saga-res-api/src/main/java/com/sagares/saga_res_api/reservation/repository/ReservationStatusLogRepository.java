package  com.sagares.saga_res_api.reservation.repository;


import com.sagares.saga_res_api.reservation.entity.ReservationStatusLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservationStatusLogRepository extends JpaRepository<ReservationStatusLog, Long> {

    List<ReservationStatusLog> findByReservationIdOrderByChangedAtAsc(Long reservationId);
}
