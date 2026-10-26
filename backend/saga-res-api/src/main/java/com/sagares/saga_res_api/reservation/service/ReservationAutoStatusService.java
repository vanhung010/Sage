package com.sagares.saga_res_api.reservation.service;

import com.sagares.saga_res_api.reservation.entity.ReservationStatus;
import com.sagares.saga_res_api.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationAutoStatusService {

    private final ReservationRepository reservationRepository;
    private final ReservationSettingsService settingsService;
    private final ReservationAutoStatusTransaction transaction;
    private final Clock clock;

    @Value("${app.reservation.auto-status.batch-size:100}")
    private int batchSize;

    public void run() {
        var settings = settingsService.loadStaffOperationSettings();
        Instant now = clock.instant();

        process(ReservationStatus.PENDING,
                now.plus(settings.pendingAutoCancelHours(), ChronoUnit.MINUTES),
                transaction::rejectExpiredPending);

        process(ReservationStatus.CONFIRMED,
                now.minus(settings.noShowGraceMinutes(), ChronoUnit.MINUTES),
                transaction::markNoShow);
    }

    private void process(ReservationStatus status, Instant threshold,
                         Consumer<Long> action) {
        List<Long> ids = reservationRepository
                .findIdsByStatusAndStartTimeAtOrBefore(
                        status, threshold, PageRequest.ofSize(batchSize));

        for (Long id : ids) {
            try {
                action.accept(id);
            } catch (Exception exception) {
                // Một đơn lỗi không được chặn các đơn còn lại.
                log.error("Không thể tự cập nhật đơn {} (trạng thái {})",
                        id, status, exception);
            }
        }

        if (ids.size() == batchSize) {
            log.warn("Còn nhiều đơn {} đến hạn; phần còn lại xử lý ở lượt sau", status);
        }
    }
}
