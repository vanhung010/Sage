package com.sagares.saga_res_api.reservation.service;

import com.sagares.saga_res_api.common.exception.BusinessException;
import com.sagares.saga_res_api.reservation.dto.CreateReservationRequest;
import com.sagares.saga_res_api.reservation.dto.ReservationResponse;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private static final int MAX_ATTEMPTS = 3;

    private final ReservationCreationTransaction creationTransaction;

    // Không đặt @Transactional ở đây.
    // Mỗi lần gọi creationTransaction.create(...) có transaction riêng.
    public ReservationResponse create(
            Long customerId,
            CreateReservationRequest request
    ) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return creationTransaction.create(customerId, request);

            } catch (DataIntegrityViolationException exception) {
                String constraint = findConstraintName(exception);

                if ("reservation_customer_no_overlap".equals(constraint)) {
                    throw new BusinessException(
                            HttpStatus.CONFLICT,
                            "CUSTOMER_TIME_CONFLICT",
                            "Bạn đã có đơn đặt bàn trùng khoảng thời gian này"
                    );
                }

                boolean tableConflict =
                        "reservation_table_no_overlap".equals(constraint);

                boolean codeConflict =
                        "reservation_code_key".equals(constraint);

                if (!tableConflict && !codeConflict) {
                    // Không retry mù quáng lỗi FK, dữ liệu thiếu hoặc lỗi khác.
                    throw exception;
                }

                // Đến đây transaction cũ đã rollback hoàn toàn.
                //
                // Nếu bàn vừa bị người khác đặt:
                // lần tiếp theo đọc lại dữ liệu, bỏ qua bàn đã bận
                // và tiếp tục tìm bàn phù hợp khác.
                //
                // Nếu mã đơn trùng:
                // lần tiếp theo sẽ sinh mã mới.
                //
                // Không catch lỗi insert rồi tiếp tục query trong cùng
                // transaction đã lỗi: PostgreSQL không cho phép điều đó.

            } catch (PessimisticLockingFailureException exception) {
                // Ví dụ deadlock hoặc không lấy được khóa.
                // Thử lại toàn bộ thao tác trong transaction mới.
            }
        }

        throw new BusinessException(
                HttpStatus.CONFLICT,
                "RESERVATION_RETRY_REQUIRED",
                "Chưa thể hoàn tất đặt bàn do có cập nhật đồng thời. "
                        + "Vui lòng thử lại"
        );
    }

    private String findConstraintName(Throwable exception) {
        Throwable current = exception;

        while (current != null) {
            if (current instanceof ConstraintViolationException violation) {
                return violation.getConstraintName();
            }

            current = current.getCause();
        }

        return null;
    }
}