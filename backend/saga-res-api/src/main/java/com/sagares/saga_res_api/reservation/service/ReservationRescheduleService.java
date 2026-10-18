package com.sagares.saga_res_api.reservation.service;

import com.sagares.saga_res_api.common.exception.BusinessException;
import com.sagares.saga_res_api.reservation.dto.RescheduleReservationRequest;
import com.sagares.saga_res_api.reservation.dto.ReservationResponse;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReservationRescheduleService {

    private final ReservationRescheduleTransaction transaction;

    // Không đặt @Transactional ở đây.
    public ReservationResponse reschedule(
            Long customerId,
            Long id,
            RescheduleReservationRequest request
    ) {
        if (id == null || id <= 0) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_RESERVATION_ID",
                    "ID đơn đặt bàn phải lớn hơn 0"
            );
        }

        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                return transaction.reschedule(customerId, id, request);

            } catch (DataIntegrityViolationException error) {
                String constraint = constraintName(error);

                if ("reservation_customer_no_overlap".equals(constraint)) {
                    throw new BusinessException(
                            HttpStatus.CONFLICT,
                            "CUSTOMER_TIME_CONFLICT",
                            "Bạn đã có đơn trùng lịch mới"
                    );
                }

                if (!"reservation_table_no_overlap".equals(constraint)) {
                    throw error;
                }

                // Transaction cũ đã rollback.
                // Lần tiếp theo đọc dữ liệu và chọn lại bàn.

            } catch (PessimisticLockingFailureException error) {
                // Retry toàn bộ transaction khi deadlock hoặc lỗi lấy khóa.
            }
        }

        throw new BusinessException(
                HttpStatus.CONFLICT,
                "RESERVATION_RETRY_REQUIRED",
                "Có cập nhật đồng thời, vui lòng tải lại đơn và thử lại"
        );
    }

    private String constraintName(Throwable error) {
        for (Throwable cause = error;
             cause != null;
             cause = cause.getCause()) {

            if (cause instanceof ConstraintViolationException violation) {
                return violation.getConstraintName();
            }
        }

        return null;
    }
}