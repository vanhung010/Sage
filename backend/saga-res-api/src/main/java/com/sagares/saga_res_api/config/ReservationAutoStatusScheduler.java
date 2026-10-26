package com.sagares.saga_res_api.config;

import com.sagares.saga_res_api.reservation.service.ReservationAutoStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.reservation.auto-status.enabled",
        havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class ReservationAutoStatusScheduler {
    private final ReservationAutoStatusService service;

    @Scheduled(
            fixedDelayString = "${app.reservation.auto-status.fixed-delay:60s}",
            initialDelayString = "${app.reservation.auto-status.initial-delay:30s}")
    public void run() {
        try {
            service.run();
        } catch (Exception exception) {
            // Lỗi cấu hình hoặc mất kết nối DB không được làm chết bộ lập lịch.
            log.error("Job tự cập nhật trạng thái đơn đặt bàn thất bại", exception);
        }
    }
}
