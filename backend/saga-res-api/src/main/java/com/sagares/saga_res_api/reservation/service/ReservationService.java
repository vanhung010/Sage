package com.sagares.saga_res_api.reservation.service;

import com.sagares.saga_res_api.account.entity.Account;
import com.sagares.saga_res_api.account.repository.AccountRepository;
import com.sagares.saga_res_api.common.exception.BusinessException;
import com.sagares.saga_res_api.reservation.dto.CreateReservationRequest;
import com.sagares.saga_res_api.reservation.dto.ReservationResponse;
import com.sagares.saga_res_api.reservation.entity.Reservation;
import com.sagares.saga_res_api.reservation.entity.ReservationLogEventType;
import com.sagares.saga_res_api.reservation.entity.ReservationStatus;
import com.sagares.saga_res_api.reservation.entity.ReservationStatusLog;
import com.sagares.saga_res_api.reservation.repository.ReservationRepository;
import com.sagares.saga_res_api.reservation.repository.ReservationStatusLogRepository;
import com.sagares.saga_res_api.security.AuthenticatedUser;
import com.sagares.saga_res_api.table.entity.Area;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private static final int MAX_ATTEMPTS = 3;

    private final ReservationCreationTransaction creationTransaction;

    private final ReservationRepository reservationRepository;

    private final AccountRepository accountRepository;

    private final ReservationStatusLogRepository reservationStatusLogRepository;

    private static final ZoneId TIME_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
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

    @Transactional
    public ReservationResponse cancel(AuthenticatedUser currentUser, Long reservationId) {


        //Chuyển trạng thái sang CANCELLED.
        //Ghi nhật ký: người thực hiện, thời điểm, trạng thái trước và sau.
        //Lưu thay đổi và nhật ký trong cùng một transaction.
        //Trả kết quả thành công.

        Account currentAccount = accountRepository.findById(currentUser.accountId()).orElseThrow(() -> new BusinessException(
                HttpStatus.UNAUTHORIZED,
                "ACCOUNT_NOT_FOUND",
                "Tài khoản không còn tồn tại"
        ));
        Reservation reservation = reservationRepository.findById(reservationId).orElseThrow(() ->
                new BusinessException(HttpStatus.NOT_FOUND,
                "RESERVATION NOT EXITS", "Không tìm thấy đơn đặt bàn"));

        if (reservation.getCustomer() == null
                || !currentUser.accountId()
                .equals(reservation.getCustomer().getId())) {
            // Không tìm thấy đơn thuộc người dùng này.
            throw new BusinessException(HttpStatus.NOT_FOUND, "USER NOT AUTHORIED", "Không tìm thấy đơn đặt bàn");
        }

        if(reservation.getStatus() != ReservationStatus.PENDING && reservation.getStatus() != ReservationStatus.CONFIRMED){
            throw new BusinessException(HttpStatus.CONFLICT, "NOT CACELLED", "Trạng thái hiện tại không cho phép hủy");
        }

        Instant now = Instant.now();

        if(!now.plus(Duration.ofHours(2)).isAfter(reservation.getStartTime())){
            throw new BusinessException(HttpStatus.CONFLICT, "NOT CANCELLED","Hiện tại không thể hủy đơn đặt bàn");
        }

        //trạng thái cũ
        ReservationStatus previousStatus = reservation.getStatus();

        reservation.setStatus(ReservationStatus.CANCELLED);

        ReservationStatusLog log = new ReservationStatusLog();
        log.setReservation(reservation);
        log.setEventType(ReservationLogEventType.STATUS_CHANGED);
        log.setOldValue(Map.of("status", previousStatus.name()));
        log.setNewValue(Map.of("status", ReservationStatus.CANCELLED.name()));

// Account của người đang đăng nhập và thực hiện hủy.
        log.setChangedBy(currentAccount);
        // Thời điểm thay đổi.
        log.setChangedAt(Instant.now());

// Lưu đơn và nhật ký.
        reservationRepository.save(reservation);
        reservationStatusLogRepository.save(log);

        return toDTO(reservation);
    }

        public static ReservationResponse toDTO(Reservation reservation) {
            Area area = reservation.getRequestedArea();

            ReservationResponse.AreaSummary requestedArea =
                    area == null
                            ? null
                            : new ReservationResponse.AreaSummary(
                            area.getId(),
                            area.getName(),
                            area.getDescription()
                    );

            return new ReservationResponse(
                    reservation.getId(),
                    reservation.getCode(),
                    reservation.getPartySize().intValue(),
                    reservation.getStartTime()
                            .atZone(TIME_ZONE)
                            .toOffsetDateTime(),
                    reservation.getEndTime()
                            .atZone(TIME_ZONE)
                            .toOffsetDateTime(),
                    reservation.getStatus(),
                    requestedArea,
                    reservation.getSpecialNote(),
                    reservation.getRescheduleCount(),
                    reservation.getRejectionReason(),
                    reservation.getCancellationReason(),
                    reservation.getCreatedAt()
                            .atZone(TIME_ZONE)
                            .toOffsetDateTime()
            );
        }
    }
