package com.sagares.saga_res_api.reservation.service;

import com.sagares.saga_res_api.account.entity.Account;
import com.sagares.saga_res_api.common.dto.PageResponse;
import com.sagares.saga_res_api.common.exception.BusinessException;
import com.sagares.saga_res_api.reservation.dto.*;
import com.sagares.saga_res_api.reservation.entity.Reservation;
import com.sagares.saga_res_api.reservation.entity.ReservationStatus;
import com.sagares.saga_res_api.reservation.entity.ReservationStatusLog;
import com.sagares.saga_res_api.reservation.event.ReservationRejectedEvent;
import com.sagares.saga_res_api.reservation.repository.ReservationRepository;
import com.sagares.saga_res_api.reservation.repository.ReservationStatusLogRepository;
import com.sagares.saga_res_api.reservation.specification.ReservationSpecifications;
import com.sagares.saga_res_api.table.entity.Area;
import com.sagares.saga_res_api.table.entity.RestaurantTable;

import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.temporal.ChronoUnit;
import com.sagares.saga_res_api.reservation.event.ReservationCancelledEvent;

import com.sagares.saga_res_api.account.repository.AccountRepository;
import com.sagares.saga_res_api.reservation.entity.ReservationLogEventType;
import com.sagares.saga_res_api.reservation.event.ReservationConfirmedEvent;
import com.sagares.saga_res_api.security.AuthenticatedUser;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;

import java.time.Clock;
import java.util.Map;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import com.sagares.saga_res_api.reservation.dto.StaffReservationDurationDto.*;
import org.springframework.dao.OptimisticLockingFailureException;

@Service
@RequiredArgsConstructor
public class StaffReservationService {

    private static final ZoneId RESTAURANT_ZONE =
            ZoneId.of("Asia/Ho_Chi_Minh");

    private final ReservationRepository reservationRepository;
    private final ReservationStatusLogRepository statusLogRepository;
    private final ReservationSettingsService settingsService;
    private final StaffReservationDurationTransaction durationTransaction;

    private final AccountRepository accountRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    private static final int MAX_ATTEMPTS = 3;


    private final StaffReservationCreationTransaction creationTransaction;

    @Transactional(readOnly = true)
    public PageResponse<StaffReservationSummaryResponse> list(
            LocalDate date,
            String search,
            ReservationStatus status,
            Long areaId,
            int page,
            int size
    ) {
        validateRequest(date, areaId, page, size);

        // Ngày được hiểu theo múi giờ nhà hàng.
        // Entity lưu Instant nên chuyển hai mốc này sang Instant.
        Instant from = date
                .atStartOfDay(RESTAURANT_ZONE)
                .toInstant();

        Instant to = date
                .plusDays(1)
                .atStartOfDay(RESTAURANT_ZONE)
                .toInstant();

        Specification<Reservation> specification =
                ReservationSpecifications.startsWithin(from, to)
                        .and(ReservationSpecifications.matchesSearch(search))
                        .and(ReservationSpecifications.hasStatus(status))
                        .and(ReservationSpecifications.inArea(areaId));

        // Không nhận sort từ client.
        // startTime tăng dần, nếu trùng thì id tăng dần.
        Sort sort = Sort.by(
                Sort.Direction.ASC,
                "startTime",
                "id"
        );

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Reservation> result =
                reservationRepository.findAll(
                        specification,
                        pageable
                );

        return PageResponse.from(result.map(this::toResponse));
    }


    @Transactional(readOnly = true)
    public StaffReservationStatisticsResponse getSummary(
            LocalDate date,
            String search,
            ReservationStatus status,
            Long areaId
    ) {
        // Kiểm tra đầu vào trước khi xử lý trường hợp trả về 0.
        if (date == null) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "MISSING_PARAMETER",
                    "Ngày khách đến dùng bữa là bắt buộc"
            );
        }

        if (areaId != null && areaId <= 0) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_AREA_ID",
                    "areaId phải lớn hơn 0"
            );
        }

        // Nếu bảng đang lọc trạng thái khác PENDING,
        // kết quả đang xem chắc chắn không có đơn chờ duyệt.
        // Không cần truy vấn database để đếm.
        if (status != null && status != ReservationStatus.PENDING) {
            return new StaffReservationStatisticsResponse(0L);
        }

        // Dùng cùng múi giờ và khoảng thời gian với API danh sách.
        Instant from = date
                .atStartOfDay(RESTAURANT_ZONE)
                .toInstant();

        Instant to = date
                .plusDays(1)
                .atStartOfDay(RESTAURANT_ZONE)
                .toInstant();

        Specification<Reservation> specification =
                ReservationSpecifications.startsWithin(from, to)
                        .and(
                                ReservationSpecifications.matchesSearch(search)
                        )
                        .and(
                                ReservationSpecifications.inArea(areaId)
                        )
                        .and(
                                ReservationSpecifications.hasStatus(
                                        ReservationStatus.PENDING
                                )
                        );

        // COUNT trực tiếp tại database.
        // Không tải danh sách entity và không áp dụng phân trang.
        long pendingCount = reservationRepository.count(specification);

        return new StaffReservationStatisticsResponse(pendingCount);
    }

    public CreateGuestReservationResponse create(
            Long staffId,
            CreateGuestReservationRequest request
    ) {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            try {
                return creationTransaction.create(staffId, request);

            } catch (DataIntegrityViolationException exception) {
                String constraint = findConstraintName(exception);

                boolean tableConflict =
                        "reservation_table_no_overlap".equals(constraint);

                boolean codeConflict =
                        "reservation_code_key".equals(constraint);

                if (!tableConflict && !codeConflict) {
                    throw exception;
                }

                // Transaction trước đã rollback.
                // Thử lại việc chọn bàn hoặc sinh mã đơn mới.

            } catch (PessimisticLockingFailureException exception) {
                // Retry toàn bộ transaction khi gặp tranh chấp khóa.
            }
        }

        throw new BusinessException(
                HttpStatus.CONFLICT,
                "RESERVATION_RETRY_REQUIRED",
                "Có cập nhật đồng thời, vui lòng thử lại"
        );
    }

    @Transactional(readOnly = true)
    public StaffReservationDetailResponse getDetail(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_RESERVATION_ID",
                    "ID đơn đặt bàn phải lớn hơn 0"
            );
        }

        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "RESERVATION_NOT_FOUND",
                        "Không tìm thấy đơn đặt bàn"
                ));

        var settings = settingsService.loadStaffOperationSettings();

        OffsetDateTime approvalDeadline = null;
        OffsetDateTime noShowAllowedAt = null;

        if (reservation.getStatus() == ReservationStatus.PENDING) {
            approvalDeadline = toDetailTime(
                    reservation.getStartTime().minus(
                            settings.pendingAutoCancelHours(),
                            ChronoUnit.HOURS
                    )
            );
        }

        if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
            noShowAllowedAt = toDetailTime(
                    reservation.getStartTime().plus(
                            settings.noShowGraceMinutes(),
                            ChronoUnit.MINUTES
                    )
            );
        }

        Account customer = reservation.getCustomer();
        RestaurantTable table = reservation.getTable();
        Area tableArea = table.getArea();
        Area requestedArea = reservation.getRequestedArea();

        // Dùng cùng field cho khách có tài khoản và khách vãng lai.
        String customerName = customer != null
                ? customer.getFullName()
                : reservation.getGuestName();

        String customerPhone = customer != null
                ? customer.getPhone()
                : reservation.getGuestPhone();

        List<StaffReservationDetailResponse.LogResponse> logs = statusLogRepository
                .findByReservationIdOrderByChangedAtAsc(id)
                .stream()
                // Timeline hiển thị mới nhất trước.
                .sorted(
                        Comparator.comparing(
                                ReservationStatusLog::getChangedAt
                        ).thenComparing(
                                ReservationStatusLog::getId
                        ).reversed()
                )
                .map(log -> {
                    Account actor = log.getChangedBy();

                    return new StaffReservationDetailResponse.LogResponse(
                            log.getId(),
                            log.getEventType(),
                            toDetailTime(log.getChangedAt()),

                            actor == null
                                    ? "Hệ thống"
                                    : actor.getFullName(),

                            actor == null
                                    ? null
                                    : actor.getRole(),

                            log.getOldValue(),
                            log.getNewValue(),
                            log.getReason()
                    );
                })
                .toList();

        return new StaffReservationDetailResponse(
                reservation.getId(),
                reservation.getCode(),
                reservation.getStatus(),

                customer == null
                        ? StaffReservationDetailResponse.BookingSource.STAFF_GUEST
                        : StaffReservationDetailResponse.BookingSource.ONLINE,

                customerName,
                customerPhone,

                reservation.getPartySize().intValue(),
                toDetailTime(reservation.getStartTime()),
                toDetailTime(reservation.getEndTime()),

                new StaffReservationDetailResponse.TableSummary(
                        table.getId(),
                        table.getCode(),
                        new StaffReservationDetailResponse.AreaSummary(
                                tableArea.getId(),
                                tableArea.getName()
                        )
                ),

                requestedArea == null
                        ? null
                        : new StaffReservationDetailResponse.AreaSummary(
                        requestedArea.getId(),
                        requestedArea.getName()
                ),

                reservation.getSpecialNote(),
                reservation.getBufferMinutes().intValue(),
                reservation.getRescheduleCount(),

                reservation.getCancellationReason(),
                reservation.getRejectionReason(),

                toDetailTime(reservation.getCreatedAt()),
                toDetailTime(reservation.getUpdatedAt()),

                approvalDeadline,
                noShowAllowedAt,

                logs
        );
    }


    @Transactional
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public StaffReservationDetailResponse approve(
            AuthenticatedUser currentUser,
            Long id
    ) {
        // 1. Kiểm tra ID trước khi truy vấn database.
        if (id == null || id <= 0) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_RESERVATION_ID",
                    "ID đơn đặt bàn phải lớn hơn 0"
            );
        }

        // 2. Lấy tài khoản đang thực hiện để ghi người duyệt.
        // accountId phải đến từ principal đã được JWT filter xác thực.
        Account actor = accountRepository
                .findById(currentUser.accountId())
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.UNAUTHORIZED,
                        "ACCOUNT_NOT_FOUND",
                        "Tài khoản không còn tồn tại"
                ));

        // 3. Khóa reservation đến hết transaction.
        // Hai request cùng duyệt một đơn sẽ phải xử lý lần lượt.
        Reservation reservation = reservationRepository
                .findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "RESERVATION_NOT_FOUND",
                        "Không tìm thấy đơn đặt bàn"
                ));

        // 4. Chỉ đơn chờ duyệt mới được xác nhận.
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "RESERVATION_NOT_PENDING",
                    "Chỉ có thể duyệt đơn đặt bàn đang chờ duyệt"
            );
        }

        // 5. Đọc thời hạn từ business_config.
        // Không hard-code mốc 3 giờ trong service.
        var settings = settingsService.loadStaffOperationSettings();

        Instant approvalDeadline = reservation.getStartTime().minus(settings.pendingAutoCancelHours(), ChronoUnit.HOURS);

        // Lấy thời gian sau khi đã lấy được khóa,
        // tránh dùng thời gian cũ nếu request phải chờ khóa.
        Instant now = clock.instant();

        // Đúng mốc hạn duyệt hoặc sau đó đều không được duyệt.
        if (!now.isBefore(approvalDeadline)) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "RESERVATION_APPROVAL_DEADLINE_PASSED",
                    "Đơn đặt bàn đã hết hạn duyệt"
            );
        }

        // 6. Giữ trạng thái trước khi thay đổi để ghi log.
        ReservationStatus previousStatus = reservation.getStatus();

        reservation.setStatus(ReservationStatus.CONFIRMED);

        // Flush cập nhật xuống database, đồng thời cập nhật
        // updatedAt thông qua JPA auditing trước khi dựng DTO.
        reservationRepository.saveAndFlush(reservation);

        // 7. Lưu nhật ký trong cùng transaction với reservation.
        ReservationStatusLog log = new ReservationStatusLog();
        log.setReservation(reservation);
        log.setEventType(ReservationLogEventType.STATUS_CHANGED);
        log.setOldValue(Map.of("status", previousStatus.name()));
        log.setNewValue(Map.of(
                "status",
                ReservationStatus.CONFIRMED.name()
        ));
        log.setChangedBy(actor);
        log.setChangedAt(now);

        statusLogRepository.saveAndFlush(log);

        // 8. Tái sử dụng hàm dựng chi tiết hiện có.
        // Log vừa lưu sẽ xuất hiện trong danh sách logs.
        StaffReservationDetailResponse response = getDetail(id);

        // 9. Phát sự kiện trong transaction.
        // Listener AFTER_COMMIT hiện có chỉ gửi email
        // khi toàn bộ transaction commit thành công.
        Account customer = reservation.getCustomer();

        if (customer != null) {
            eventPublisher.publishEvent(
                    new ReservationConfirmedEvent(
                            customer.getEmail(),
                            ReservationService.toDTO(reservation)
                    )
            );
        }

        return response;
    }

    @Transactional
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public StaffReservationDetailResponse reject(AuthenticatedUser currentUser, Long id, RejectReservationRequest request){
        // 1. Kiểm tra ID.
        if (id == null || id <= 0) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_RESERVATION_ID",
                    "ID đơn đặt bàn phải lớn hơn 0"
            );
        }

        // 2. Kiểm tra thêm tại service để bảo vệ các lời gọi nội bộ.
        if (request == null
                || request.reason() == null
                || request.reason().isBlank()) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "REJECTION_REASON_REQUIRED",
                    "Lý do từ chối không được để trống"
            );
        }

        // Loại bỏ khoảng trắng ở đầu và cuối trước khi lưu.
        String reason = request.reason().strip();

        // 3. Lấy tài khoản thực hiện từ principal đã xác thực.
        Account actor = accountRepository
                .findById(currentUser.accountId())
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.UNAUTHORIZED,
                        "ACCOUNT_NOT_FOUND",
                        "Tài khoản không còn tồn tại"
                ));

        // 4. Khóa reservation để tránh duyệt/từ chối đồng thời.
        Reservation reservation = reservationRepository
                .findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "RESERVATION_NOT_FOUND",
                        "Không tìm thấy đơn đặt bàn"
                ));

        // 5. Kiểm tra trạng thái sau khi lấy được khóa.
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "RESERVATION_NOT_PENDING",
                    "Chỉ có thể từ chối đơn đặt bàn đang chờ duyệt"
            );
        }

        Instant now = clock.instant();
        ReservationStatus previousStatus = reservation.getStatus();

        reservation.setStatus(ReservationStatus.REJECTED);
        reservation.setRejectionReason(reason);

        // Flush để updatedAt được cập nhật trước khi dựng response.
        reservationRepository.saveAndFlush(reservation);

        // 7. Lưu nhật ký trong cùng transaction.
        ReservationStatusLog log = new ReservationStatusLog();
        log.setReservation(reservation);
        log.setEventType(ReservationLogEventType.STATUS_CHANGED);

        log.setOldValue(Map.of(
                "status", previousStatus.name()
        ));

        log.setNewValue(Map.of(
                "status", ReservationStatus.REJECTED.name(),
                "rejectionReason", reason
        ));

        log.setReason(reason);
        log.setChangedBy(actor);
        log.setChangedAt(now);

        statusLogRepository.saveAndFlush(log);

        // 8. Thông báo cho khách sau khi transaction commit.
        Account customer = reservation.getCustomer();

        if (customer != null) {
            eventPublisher.publishEvent(
                    new ReservationRejectedEvent(
                            customer.getEmail(),
                            reservation.getCode(),
                            reason
                    )
            );
        }
        return getDetail(id);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public StaffReservationDetailResponse cancelForCustomer(
            AuthenticatedUser currentUser,
            Long id,
            StaffCancelReservationRequest request
    ) {
        // 1. Kiểm tra ID.
        if (id == null || id <= 0) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_RESERVATION_ID",
                    "ID đơn đặt bàn phải lớn hơn 0"
            );
        }

        // 2. Bảo vệ cả trường hợp service được gọi trực tiếp.
        if (request == null
                || request.reason() == null
                || request.reason().isBlank()) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "CANCELLATION_REASON_REQUIRED",
                    "Lý do hủy không được để trống"
            );
        }

        String reason = request.reason().strip();

        // 3. Lấy nhân viên đang thao tác để ghi nhật ký.
        Account actor = accountRepository
                .findById(currentUser.accountId())
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.UNAUTHORIZED,
                        "ACCOUNT_NOT_FOUND",
                        "Tài khoản không còn tồn tại"
                ));

        // 4. Khóa đơn để xử lý tuần tự với duyệt, từ chối,
        // đổi lịch hoặc một request hủy khác trên cùng đơn.
        Reservation reservation = reservationRepository
                .findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "RESERVATION_NOT_FOUND",
                        "Không tìm thấy đơn đặt bàn"
                ));

        // 5. Chỉ hai trạng thái này được phép hủy.
        ReservationStatus previousStatus = reservation.getStatus();

        if (previousStatus != ReservationStatus.PENDING
                && previousStatus != ReservationStatus.CONFIRMED) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "RESERVATION_NOT_CANCELLABLE",
                    "Chỉ có thể hủy đơn đang chờ duyệt hoặc đã xác nhận"
            );
        }

        // Staff/Admin không cần kiểm tra ownership
        // và không chịu hạn hủy 2 giờ của CUSTOMER.
        Instant now = clock.instant();

        // 6. Cập nhật trạng thái và lý do hủy.
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setCancellationReason(reason);

        // Flush để cập nhật updatedAt trước khi dựng response.
        reservationRepository.saveAndFlush(reservation);

        // 7. Ghi log cùng transaction với reservation.
        ReservationStatusLog log = new ReservationStatusLog();
        log.setReservation(reservation);
        log.setEventType(ReservationLogEventType.STATUS_CHANGED);

        log.setOldValue(Map.of(
                "status", previousStatus.name()
        ));

        log.setNewValue(Map.of(
                "status", ReservationStatus.CANCELLED.name(),
                "cancellationReason", reason
        ));

        log.setReason(reason);
        log.setChangedBy(actor);
        log.setChangedAt(now);

        statusLogRepository.saveAndFlush(log);

        // 8. Chỉ đơn gắn tài khoản khách mới có email.
        // Đơn đặt hộ khách vãng lai không gửi email.
        Account customer = reservation.getCustomer();

        if (customer != null) {
            eventPublisher.publishEvent(
                    new ReservationCancelledEvent(
                            customer.getEmail(),
                            reservation.getCode(),
                            reason
                    )
            );
        }

        // 9. Response có trạng thái, lý do hủy và log vừa tạo.
        return getDetail(id);
    }


    @Transactional
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public StaffReservationDetailResponse markArrived(
            AuthenticatedUser currentUser,
            Long id
    ) {
        // 1. Kiểm tra ID trước khi truy vấn.
        if (id == null || id <= 0) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_RESERVATION_ID",
                    "ID đơn đặt bàn phải lớn hơn 0"
            );
        }

        // 2. Lấy người thực hiện để ghi nhật ký.
        Account actor = accountRepository
                .findById(currentUser.accountId())
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.UNAUTHORIZED,
                        "ACCOUNT_NOT_FOUND",
                        "Tài khoản không còn tồn tại"
                ));

        // 3. Khóa reservation để tránh cập nhật đồng thời.
        Reservation reservation = reservationRepository
                .findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "RESERVATION_NOT_FOUND",
                        "Không tìm thấy đơn đặt bàn"
                ));

        // 4. Chỉ đơn đã xác nhận mới được ghi nhận khách đến.
        ReservationStatus previousStatus = reservation.getStatus();

        if (previousStatus != ReservationStatus.CONFIRMED) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "RESERVATION_NOT_CONFIRMED",
                    "Chỉ có thể ghi nhận khách đến cho đơn đã xác nhận"
            );
        }

        // 5. Lấy thời gian sau khi đã lấy được khóa.
        // Dùng cùng một mốc now để tránh lệch ngày tại thời điểm nửa đêm.
        Instant now = clock.instant();

        LocalDate today = now
                .atZone(RESTAURANT_ZONE)
                .toLocalDate();

        LocalDate reservationDate = reservation.getStartTime()
                .atZone(RESTAURANT_ZONE)
                .toLocalDate();

        if (!today.equals(reservationDate)) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "RESERVATION_ARRIVAL_DATE_MISMATCH",
                    "Chỉ được ghi nhận khách đến trong ngày hẹn"
            );
        }

        // 6. Cập nhật trạng thái.
        // Giữ nguyên bàn, giờ bắt đầu, giờ kết thúc và buffer.
        reservation.setStatus(ReservationStatus.ARRIVED);

        // Flush để cập nhật updatedAt trước khi dựng response.
        reservationRepository.saveAndFlush(reservation);

        // 7. Ghi nhật ký trong cùng transaction.
        ReservationStatusLog log = new ReservationStatusLog();
        log.setReservation(reservation);
        log.setEventType(ReservationLogEventType.STATUS_CHANGED);

        log.setOldValue(Map.of(
                "status", previousStatus.name()
        ));

        log.setNewValue(Map.of(
                "status", ReservationStatus.ARRIVED.name()
        ));

        log.setChangedBy(actor);
        log.setChangedAt(now);

        statusLogRepository.saveAndFlush(log);

        // 8. Response bao gồm trạng thái và log vừa tạo.
        return getDetail(id);
    }


    @Transactional
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public StaffReservationDetailResponse markNoShow(
            AuthenticatedUser currentUser,
            Long id
    ) {
        // 1. Kiểm tra ID.
        if (id == null || id <= 0) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_RESERVATION_ID",
                    "ID đơn đặt bàn phải lớn hơn 0"
            );
        }

        // 2. Lấy người thực hiện để ghi nhật ký.
        Account actor = accountRepository
                .findById(currentUser.accountId())
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.UNAUTHORIZED,
                        "ACCOUNT_NOT_FOUND",
                        "Tài khoản không còn tồn tại"
                ));

        // 3. Khóa reservation để tránh tranh chấp
        // với ghi nhận khách đến, hủy hoặc request NO_SHOW khác.
        Reservation reservation = reservationRepository
                .findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "RESERVATION_NOT_FOUND",
                        "Không tìm thấy đơn đặt bàn"
                ));

        // 4. Kiểm tra trạng thái sau khi lấy được khóa.
        ReservationStatus previousStatus = reservation.getStatus();

        if (previousStatus != ReservationStatus.CONFIRMED) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "RESERVATION_NOT_CONFIRMED",
                    "Chỉ có thể ghi nhận không đến cho đơn đã xác nhận"
            );
        }

        // 5. Đọc số phút chờ từ business_config.
        var settings = settingsService.loadStaffOperationSettings();

        Instant noShowAllowedAt = reservation.getStartTime()
                .plus(
                        settings.noShowGraceMinutes(),
                        ChronoUnit.MINUTES
                );

        // Lấy thời gian sau khi đã lấy được khóa.
        Instant now = clock.instant();

        // Đúng mốc noShowAllowedAt thì được phép thực hiện.
        if (now.isBefore(noShowAllowedAt)) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "NO_SHOW_NOT_ALLOWED_YET",
                    "Chưa đủ thời gian chờ. Có thể ghi nhận khách "
                            + "không đến từ "
                            + toDetailTime(noShowAllowedAt)
            );
        }

        // 6. Cập nhật trạng thái, giữ nguyên thông tin đặt bàn.
        reservation.setStatus(ReservationStatus.NO_SHOW);

        // Flush để cập nhật updatedAt trước khi dựng response.
        reservationRepository.saveAndFlush(reservation);

        // 7. Ghi nhật ký trong cùng transaction.
        ReservationStatusLog log = new ReservationStatusLog();
        log.setReservation(reservation);
        log.setEventType(ReservationLogEventType.STATUS_CHANGED);

        log.setOldValue(Map.of(
                "status", previousStatus.name()
        ));

        log.setNewValue(Map.of(
                "status", ReservationStatus.NO_SHOW.name()
        ));

        log.setChangedBy(actor);
        log.setChangedAt(now);

        statusLogRepository.saveAndFlush(log);

        // 8. Response bao gồm trạng thái và log vừa tạo.
        return getDetail(id);
    }


    @Transactional
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public StaffReservationDetailResponse markCompleted(
            AuthenticatedUser currentUser,
            Long id
    ) {
        // 1. Kiểm tra ID.
        if (id == null || id <= 0) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_RESERVATION_ID",
                    "ID đơn đặt bàn phải lớn hơn 0"
            );
        }

        // 2. Lấy người thực hiện để ghi nhật ký.
        Account actor = accountRepository
                .findById(currentUser.accountId())
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.UNAUTHORIZED,
                        "ACCOUNT_NOT_FOUND",
                        "Tài khoản không còn tồn tại"
                ));

        // 3. Khóa reservation để tránh cập nhật đồng thời.
        Reservation reservation = reservationRepository
                .findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "RESERVATION_NOT_FOUND",
                        "Không tìm thấy đơn đặt bàn"
                ));

        // 4. Chỉ đơn đã ghi nhận khách đến mới được hoàn thành.
        ReservationStatus previousStatus = reservation.getStatus();

        if (previousStatus != ReservationStatus.ARRIVED) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "RESERVATION_NOT_ARRIVED",
                    "Chỉ có thể hoàn thành đơn đã ghi nhận khách đến"
            );
        }

        Instant now = clock.instant();

        // 5. Chỉ cập nhật trạng thái.
        // Giữ nguyên table, startTime, endTime và bufferMinutes.
        reservation.setStatus(ReservationStatus.COMPLETED);

        // Flush để cập nhật updatedAt trước khi dựng response.
        reservationRepository.saveAndFlush(reservation);

        // 6. Ghi nhật ký trong cùng transaction.
        ReservationStatusLog log = new ReservationStatusLog();
        log.setReservation(reservation);
        log.setEventType(ReservationLogEventType.STATUS_CHANGED);

        log.setOldValue(Map.of(
                "status", previousStatus.name()
        ));

        log.setNewValue(Map.of(
                "status", ReservationStatus.COMPLETED.name()
        ));

        log.setChangedBy(actor);
        log.setChangedAt(now);

        statusLogRepository.saveAndFlush(log);

        // 7. Response bao gồm trạng thái và log vừa tạo.
        return getDetail(id);
    }

    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public PreviewResponse previewDuration(
            Long id,
            PreviewRequest request
    ) {
        return durationTransaction.preview(id, request);
    }

    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ChangeResponse changeDuration(
            AuthenticatedUser currentUser,
            Long id,
            ChangeRequest request
    ) {
        try {
            return durationTransaction.change(
                    currentUser.accountId(),
                    id,
                    request
            );

        } catch (DataIntegrityViolationException exception) {
            String constraint = findConstraintName(exception);

            if ("reservation_table_no_overlap".equals(constraint)) {
                throw new BusinessException(
                        HttpStatus.CONFLICT,
                        "TABLE_TIME_CONFLICT",
                        "Bàn vừa có lịch trùng, vui lòng kiểm tra lại thời lượng"
                );
            }

            if ("reservation_customer_no_overlap".equals(constraint)) {
                throw new BusinessException(
                        HttpStatus.CONFLICT,
                        "CUSTOMER_TIME_CONFLICT",
                        "Khách vừa có lịch đặt bàn trùng giờ"
                );
            }

            throw exception;

        } catch (OptimisticLockingFailureException exception) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "RESERVATION_VERSION_CONFLICT",
                    "Đơn đặt bàn đã được cập nhật, vui lòng tải lại"
            );

        } catch (PessimisticLockingFailureException exception) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "RESERVATION_RETRY_REQUIRED",
                    "Có cập nhật đồng thời, vui lòng thử lại"
            );
        }
    }



    private OffsetDateTime toDetailTime(Instant value) {
        return value == null
                ? null
                : value.atZone(RESTAURANT_ZONE).toOffsetDateTime();
    }

    private String findConstraintName(Throwable exception) {
        for (Throwable cause = exception;
             cause != null;
             cause = cause.getCause()) {

            if (cause instanceof ConstraintViolationException violation) {
                return violation.getConstraintName();
            }
        }

        return null;
    }

    private void validateRequest(
            LocalDate date,
            Long areaId,
            int page,
            int size
    ) {
        if (date == null) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "MISSING_PARAMETER",
                    "Ngày khách đến dùng bữa là bắt buộc"
            );
        }

        if (page < 0 || size < 1 || size > 100) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_PAGINATION",
                    "page phải >= 0; size phải từ 1 đến 100"
            );
        }

        if (areaId != null && areaId <= 0) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_AREA_ID",
                    "areaId phải lớn hơn 0"
            );
        }
    }

    private StaffReservationSummaryResponse toResponse(
            Reservation reservation
    ) {
        Account customer = reservation.getCustomer();

        String customerName = customer != null
                ? customer.getFullName()
                : reservation.getGuestName();

        String customerPhone = customer != null
                ? customer.getPhone()
                : reservation.getGuestPhone();

        // Theo model hiện có, table và table.area đều bắt buộc.
        RestaurantTable table = reservation.getTable();
        Area area = table.getArea();

        return new StaffReservationSummaryResponse(
                reservation.getId(),
                reservation.getCode(),
                customerName,
                customerPhone,
                reservation.getStartTime(),
                reservation.getEndTime(),
                reservation.getPartySize().intValue(),
                table.getId(),
                table.getCode(),
                area.getId(),
                area.getName(),
                reservation.getStatus()
        );
    }
}