package com.sagares.saga_res_api.reservation.service;

import com.sagares.saga_res_api.common.exception.BusinessException;
import com.sagares.saga_res_api.reservation.dto.AvailabilityResponse;
import com.sagares.saga_res_api.reservation.entity.Reservation;
import com.sagares.saga_res_api.reservation.entity.ReservationStatus;
import com.sagares.saga_res_api.reservation.repository.ReservationRepository;
import com.sagares.saga_res_api.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class RescheduleAvailabilityService {

    private final ReservationRepository reservationRepository;
    private final ReservationSettingsService settingsService;
    private final AvailabilityService availabilityService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public AvailabilityResponse search(
            AuthenticatedUser currentUser,
            Long reservationId,
            LocalDate date,
            int partySize,
            int durationMinutes,
            Long areaId
    ) {
        if (reservationId == null || reservationId <= 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "INVALID_RESERVATION_ID", "ID đơn đặt bàn phải lớn hơn 0");
        }

        // Không cho client tự quyết định đơn nào được loại khỏi kiểm tra chiếm chỗ.
        Reservation reservation = reservationRepository
                .findByIdAndCustomer_Id(reservationId, currentUser.accountId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "RESERVATION_NOT_FOUND", "Không tìm thấy đơn đặt bàn"));

        if (reservation.getStatus() != ReservationStatus.PENDING
                && reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "RESERVATION_NOT_RESCHEDULABLE",
                    "Trạng thái hiện tại không cho phép đổi lịch");
        }

        var settings = settingsService.loadRescheduleSettings();
        if (reservation.getRescheduleCount() >= settings.maxRescheduleCount()) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "RESCHEDULE_LIMIT_REACHED", "Đơn đã hết số lần đổi lịch");
        }

        Instant now = clock.instant();
        // Kiểm tra hạn dựa trên lịch hiện tại, không phải ngày mới được chọn.
        // Đúng thời điểm còn 2 giờ vẫn được phép đổi.
        if (now.plus(settings.deadlineHours(), ChronoUnit.HOURS)
                .isAfter(reservation.getStartTime())) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "RESCHEDULE_DEADLINE_PASSED",
                    "Chỉ được đổi lịch khi còn ít nhất "
                            + settings.deadlineHours() + " giờ trước giờ hẹn hiện tại");
        }

        return availabilityService.searchForReschedule(
                date, partySize, durationMinutes, areaId, reservation, now);
    }
}