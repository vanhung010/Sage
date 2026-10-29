package com.sagares.saga_res_api.reservation.service;

import com.sagares.saga_res_api.account.entity.Account;
import com.sagares.saga_res_api.common.realtime.RealtimeChange;
import com.sagares.saga_res_api.common.realtime.RealtimeEvent;
import com.sagares.saga_res_api.reservation.entity.Reservation;
import com.sagares.saga_res_api.reservation.entity.ReservationLogEventType;
import com.sagares.saga_res_api.reservation.entity.ReservationStatus;
import com.sagares.saga_res_api.reservation.entity.ReservationStatusLog;
import com.sagares.saga_res_api.reservation.event.ReservationRejectedEvent;
import com.sagares.saga_res_api.reservation.repository.ReservationRepository;
import com.sagares.saga_res_api.reservation.repository.ReservationStatusLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationAutoStatusTransaction {
    public static final String AUTO_REJECT_REASON =
            "Nhà hàng không kịp xác nhận đơn trước giờ hẹn";

    private final ReservationRepository reservationRepository;
    private final ReservationStatusLogRepository statusLogRepository;
    private final ReservationSettingsService settingsService;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Transactional
    public void rejectExpiredPending(Long id) {
        // Khóa row: tranh chấp với staff duyệt/từ chối cùng lúc.
        Reservation reservation = reservationRepository
                .findByIdForUpdate(id).orElse(null);

        // Staff đã xử lý trước job, hoặc đơn không còn tồn tại.
        if (reservation == null
                || reservation.getStatus() != ReservationStatus.PENDING) {
            return;
        }

        var settings = settingsService.loadStaffOperationSettings();
        Instant now = clock.instant();
        Instant deadline = reservation.getStartTime()
                .minus(settings.pendingAutoCancelHours(), ChronoUnit.MINUTES);

        // Đơn có thể vừa được đổi lịch sang giờ muộn hơn.
        if (now.isBefore(deadline)) {
            return;
        }

        reservation.setStatus(ReservationStatus.REJECTED);
        reservation.setRejectionReason(AUTO_REJECT_REASON);
        reservationRepository.saveAndFlush(reservation);

        writeLog(reservation, ReservationStatus.PENDING,
                ReservationStatus.REJECTED, AUTO_REJECT_REASON, now);

        Account customer = reservation.getCustomer();

        eventPublisher.publishEvent(new RealtimeChange(
                RealtimeChange.Kind.RESERVATION,
                customer == null ? null : customer.getId(),
                RealtimeEvent.of(
                        "RESERVATION_UPDATED",
                        reservation.getId()
                )
        ));
        if (customer != null) {
            eventPublisher.publishEvent(new ReservationRejectedEvent(
                    customer.getEmail(),
                    reservation.getCode(),
                    AUTO_REJECT_REASON));
        }
    }

    @Transactional
    public void markNoShow(Long id) {
        Reservation reservation = reservationRepository
                .findByIdForUpdate(id).orElse(null);

        if (reservation == null
                || reservation.getStatus() != ReservationStatus.CONFIRMED) {
            return; // staff đã bấm "Đã đến", "Hủy", ... trước job
        }

        var settings = settingsService.loadStaffOperationSettings();
        Instant now = clock.instant();
        Instant allowedAt = reservation.getStartTime()
                .plus(settings.noShowGraceMinutes(), ChronoUnit.MINUTES);

        if (now.isBefore(allowedAt)) {
            return;
        }

        reservation.setStatus(ReservationStatus.NO_SHOW);
        reservationRepository.saveAndFlush(reservation);

        writeLog(reservation, ReservationStatus.CONFIRMED, ReservationStatus.NO_SHOW, null, now);

        Long customerId = reservation.getCustomer() == null
                ? null
                : reservation.getCustomer().getId();

        eventPublisher.publishEvent(new RealtimeChange(
                RealtimeChange.Kind.RESERVATION,
                customerId,
                RealtimeEvent.of(
                        "RESERVATION_UPDATED",
                        reservation.getId()
                )
        ));
        
    }

    private void writeLog(Reservation reservation,
                          ReservationStatus from, ReservationStatus to,
                          String reason, Instant now) {
        ReservationStatusLog log = new ReservationStatusLog();
        log.setReservation(reservation);
        log.setEventType(ReservationLogEventType.STATUS_CHANGED);
        log.setOldValue(Map.of("status", from.name()));

        Map<String, Object> newValue = new HashMap<>();
        newValue.put("status", to.name());
        if (reason != null) newValue.put("rejectionReason", reason);
        log.setNewValue(newValue);

        log.setReason(reason);
        log.setChangedBy(null);   // người thực hiện = hệ thống
        log.setChangedAt(now);
        statusLogRepository.saveAndFlush(log);
    }

}
