package com.sagares.saga_res_api.reservation.service;

import com.sagares.saga_res_api.account.entity.Account;
import com.sagares.saga_res_api.common.dto.PageResponse;
import com.sagares.saga_res_api.common.exception.BusinessException;
import com.sagares.saga_res_api.common.realtime.RealtimeChange;
import com.sagares.saga_res_api.common.realtime.RealtimeEvent;
import com.sagares.saga_res_api.reservation.dto.*;
import com.sagares.saga_res_api.reservation.entity.Reservation;
import com.sagares.saga_res_api.reservation.entity.ReservationStatus;
import com.sagares.saga_res_api.reservation.entity.ReservationStatusLog;
import com.sagares.saga_res_api.reservation.event.ReservationRejectedEvent;
import com.sagares.saga_res_api.reservation.repository.ReservationRepository;
import com.sagares.saga_res_api.reservation.repository.ReservationStatusLogRepository;
import com.sagares.saga_res_api.reservation.specification.ReservationSpecifications;
import com.sagares.saga_res_api.restaurant.entity.OpeningHour;
import com.sagares.saga_res_api.restaurant.entity.SpecialClosedDay;
import com.sagares.saga_res_api.restaurant.repository.OpeningHourRepository;
import com.sagares.saga_res_api.restaurant.repository.SpecialClosedDayRepository;
import com.sagares.saga_res_api.table.entity.Area;
import com.sagares.saga_res_api.table.entity.RestaurantTable;

import com.sagares.saga_res_api.table.entity.TableSuspension;
import com.sagares.saga_res_api.table.repository.AreaRepository;
import com.sagares.saga_res_api.table.repository.RestaurantTableRepository;
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
import com.sagares.saga_res_api.reservation.dto
        .StaffMoveTableOptionsResponse.*;

import com.sagares.saga_res_api.reservation.dto
        .StaffReservationDetailResponse.AreaSummary;
import com.sagares.saga_res_api.reservation.dto
        .StaffReservationDetailResponse.TableSummary;

import com.sagares.saga_res_api.table.repository.AreaRepository;
import com.sagares.saga_res_api.table.repository.RestaurantTableRepository;
import com.sagares.saga_res_api.table.repository
        .RestaurantTableRepository.MoveTableOptionRow;

import org.springframework.transaction.annotation.Isolation;

import java.util.*;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;

import java.time.Clock;

import com.sagares.saga_res_api.account.entity.AccountRole;
import com.sagares.saga_res_api.table.repository.RestaurantTableRepository;
import com.sagares.saga_res_api.table.repository.TableSuspensionRepository;

import jakarta.persistence.EntityManager;

import org.springframework.transaction.annotation.Isolation;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.stream.Collectors;

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
    private final RestaurantTableRepository restaurantTableRepository;
    private final AreaRepository areaRepository;
    private final TableSuspensionRepository tableSuspensionRepository;
    private final AccountRepository accountRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;
    private final EntityManager entityManager;
    private final RestaurantTableRepository tableRepository;
    private final OpeningHourRepository openingHourRepository;
    private final SpecialClosedDayRepository specialClosedDayRepository;
    private final TableSuspensionRepository suspensionRepository;
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

        eventPublisher.publishEvent(new RealtimeChange(
                RealtimeChange.Kind.RESERVATION,
                customer == null ? null : customer.getId(),
                RealtimeEvent.of("RESERVATION_UPDATED", reservation.getId())
        ));

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

    @Transactional(
            readOnly = true,
            isolation = Isolation.REPEATABLE_READ
    )
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public StaffMoveTableOptionsResponse getMoveTableOptions(
            Long id,
            Long areaId
    ) {
        if (id == null || id <= 0) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_RESERVATION_ID",
                    "ID đơn đặt bàn phải lớn hơn 0"
            );
        }

        if (areaId != null && areaId <= 0) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_AREA_ID",
                    "areaId phải lớn hơn 0"
            );
        }

        // Method hiện có dùng EntityGraph để tải customer,
        // table, table.area và requestedArea trong cùng truy vấn.
        Reservation reservation = reservationRepository
                .findStaffDetailById(id)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "RESERVATION_NOT_FOUND",
                        "Không tìm thấy đơn đặt bàn"
                ));

        ReservationStatus status = reservation.getStatus();

        if (status != ReservationStatus.PENDING
                && status != ReservationStatus.CONFIRMED
                && status != ReservationStatus.ARRIVED) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "MOVE_TABLE_NOT_ALLOWED",
                    "Trạng thái hiện tại không cho phép chuyển bàn"
            );
        }

        // Danh sách bộ lọc không phụ thuộc areaId đang chọn.
        List<Area> areas = areaRepository.findAllByOrderByNameAsc();

        if (areaId != null && areas.stream().noneMatch(
                area -> Objects.equals(area.getId(), areaId)
        )) {
            throw new BusinessException(
                    HttpStatus.NOT_FOUND,
                    "AREA_NOT_FOUND",
                    "Không tìm thấy khu vực"
            );
        }

        RestaurantTable currentTable = reservation.getTable();
        Area requestedArea = reservation.getRequestedArea();

        Long requestedAreaId = requestedArea == null
                ? null
                : requestedArea.getId();

        // Giữ đúng buffer đã được lưu trên đơn.
        Instant occupiedStart = reservation.getStartTime();
        Instant occupiedEnd = reservation.getEndTime().plus(
                reservation.getBufferMinutes(),
                ChronoUnit.MINUTES
        );

        List<TableOption> tables = restaurantTableRepository
                .findMoveTableOptions(
                        reservation.getId(),
                        currentTable.getId(),
                        reservation.getPartySize().intValue(),
                        areaId,
                        requestedAreaId,
                        occupiedStart,
                        occupiedEnd
                )
                .stream()
                .map(row -> toMoveTableOption(
                        row,
                        reservation.getPartySize().intValue(),
                        requestedAreaId
                ))
                .toList();

        int suitableTableCount = (int) tables.stream()
                .filter(TableOption::selectable)
                .count();

        Account customer = reservation.getCustomer();

        ReservationContext context = new ReservationContext(
                reservation.getId(),
                reservation.getCode(),
                reservation.getVersion(),
                reservation.getStatus(),

                customer == null
                        ? reservation.getGuestName()
                        : customer.getFullName(),

                reservation.getPartySize().intValue(),
                toDetailTime(reservation.getStartTime()),
                toDetailTime(reservation.getEndTime()),
                reservation.getBufferMinutes().intValue(),
                toDetailTime(occupiedEnd),

                new TableSummary(
                        currentTable.getId(),
                        currentTable.getCode(),
                        toMoveAreaSummary(currentTable.getArea())
                ),

                toMoveAreaSummary(requestedArea)
        );

        return new StaffMoveTableOptionsResponse(
                context,
                areas.stream()
                        .map(this::toMoveAreaSummary)
                        .toList(),
                areaId,
                suitableTableCount,
                tables.size(),
                tables
        );
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public StaffReservationDetailResponse moveTable(
            AuthenticatedUser currentUser,
            Long id,
            MoveTableRequest request
    ) {
        if (id == null || id <= 0) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_RESERVATION_ID",
                    "ID đơn đặt bàn phải lớn hơn 0"
            );
        }

        // Khóa đơn trước để tránh chuyển bàn, đổi thời lượng
        // hoặc thay đổi trạng thái đồng thời trên cùng một đơn.
        Reservation reservation = reservationRepository
                .findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "RESERVATION_NOT_FOUND",
                        "Không tìm thấy đơn đặt bàn"
                ));

        // Đọc lại sau khi lấy được khóa, tránh dùng entity
        // đã nằm trong persistence context với dữ liệu cũ.
        entityManager.refresh(reservation);

        if (!Objects.equals(
                request.expectedVersion(),
                reservation.getVersion()
        )) {
            throw new BusinessException(
                    "RESERVATION_VERSION_CONFLICT",
                    "Đơn đặt bàn đã được cập nhật, vui lòng tải lại"
            );
        }

        ReservationStatus status = reservation.getStatus();

        if (status != ReservationStatus.PENDING
                && status != ReservationStatus.CONFIRMED
                && status != ReservationStatus.ARRIVED) {
            throw new BusinessException(
                    "MOVE_TABLE_NOT_ALLOWED",
                    "Chỉ được chuyển bàn cho đơn đang chờ duyệt, "
                            + "đã xác nhận hoặc khách đã đến"
            );
        }

        // Không thay đổi dữ liệu hoặc tạo thêm log khi chọn bàn hiện tại.
        if (Objects.equals(
                reservation.getTable().getId(),
                request.tableId()
        )) {
            return getDetail(id);
        }

        // Cùng khóa bàn đích với luồng đặt bàn và tạo lịch tạm ngưng.
        RestaurantTable targetTable = restaurantTableRepository
                .findByIdForUpdate(request.tableId())
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "TABLE_NOT_FOUND",
                        "Không tìm thấy bàn cần chuyển đến"
                ));

        entityManager.refresh(targetTable);

        if (!targetTable.isActive()) {
            throw new BusinessException(
                    "TABLE_INACTIVE",
                    "Bàn đã ngừng hoạt động, vui lòng chọn bàn khác"
            );
        }

        int partySize = reservation.getPartySize();

        if (partySize < targetTable.getMinCapacity()
                || partySize > targetTable.getMaxCapacity()) {
            throw new BusinessException(
                    "TABLE_CAPACITY_MISMATCH",
                    "Bàn " + targetTable.getCode()
                            + " nhận từ " + targetTable.getMinCapacity()
                            + " đến " + targetTable.getMaxCapacity()
                            + " khách; đơn hiện có " + partySize + " khách"
            );
        }

        // Giữ nguyên toàn bộ thời gian dùng bữa và buffer của đơn.
        Instant occupiedStart = reservation.getStartTime();
        Instant occupiedEnd = reservation.getEndTime().plus(
                reservation.getBufferMinutes(),
                ChronoUnit.MINUTES
        );

        // Danh sách bàn ở màn hình trước chỉ là kết quả tham khảo.
        // Luôn kiểm tra lại sau khi đã khóa bàn đích.
        boolean overlapping = reservationRepository
                .existsTableOverlapExcludingReservation(
                        reservation.getId(),
                        targetTable.getId(),
                        occupiedStart,
                        occupiedEnd
                );

        if (overlapping) {
            throw new BusinessException(
                    "TABLE_TIME_CONFLICT",
                    "Bàn " + targetTable.getCode()
                            + " đã có lịch trùng, vui lòng chọn lại"
            );
        }

        boolean suspended = !tableSuspensionRepository
                .findOverlapping(
                        targetTable.getId(),
                        occupiedStart,
                        occupiedEnd
                )
                .isEmpty();

        if (suspended) {
            throw new BusinessException(
                    "TABLE_SUSPENDED",
                    "Bàn " + targetTable.getCode()
                            + " bị tạm ngưng trong khoảng thời gian này"
            );
        }

        // Người thực hiện lấy từ principal đã xác thực.
        Account actor = accountRepository
                .findById(currentUser.accountId())
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.UNAUTHORIZED,
                        "ACCOUNT_NOT_FOUND",
                        "Tài khoản không còn tồn tại"
                ));

        entityManager.refresh(actor);

        if (actor.isLocked()) {
            throw new BusinessException(
                    HttpStatus.UNAUTHORIZED,
                    "ACCOUNT_LOCKED",
                    "Tài khoản đã bị khóa"
            );
        }

        if (actor.getRole() != AccountRole.STAFF
                && actor.getRole() != AccountRole.ADMIN) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN,
                    "ACCESS_DENIED",
                    "Bạn không có quyền chuyển bàn"
            );
        }

        Map<String, Object> oldValue = moveTableSnapshot(reservation);

        // Chỉ đổi bàn được gán.
        // requestedArea, thời gian, số khách và trạng thái giữ nguyên.
        reservation.setTable(targetTable);

        // Flush để kiểm tra constraint database, cập nhật version/updatedAt.
        reservationRepository.saveAndFlush(reservation);

        ReservationStatusLog log = new ReservationStatusLog();
        log.setReservation(reservation);
        log.setEventType(ReservationLogEventType.TABLE_CHANGED);
        log.setOldValue(oldValue);
        log.setNewValue(moveTableSnapshot(reservation));
        log.setChangedBy(actor);
        log.setChangedAt(clock.instant());

        String reason = request.reason();

        log.setReason(
                reason == null || reason.isBlank()
                        ? null
                        : reason.strip()
        );

        // Nếu ghi log thất bại, việc đổi bàn cũng rollback.
        statusLogRepository.saveAndFlush(log);

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

        // Tái sử dụng response chi tiết và danh sách log hiện có.
        return getDetail(id);
    }

    private Map<String, Object> moveTableSnapshot(
            Reservation reservation
    ) {
        RestaurantTable table = reservation.getTable();
        Area tableArea = table.getArea();
        Area requestedArea = reservation.getRequestedArea();

        // Cho phép requestedAreaId là null.
        Map<String, Object> value = new LinkedHashMap<>();

        value.put("tableId", table.getId());
        value.put("tableCode", table.getCode());
        value.put("tableAreaId", tableArea.getId());
        value.put("tableAreaName", tableArea.getName());

        value.put("startTime", reservation.getStartTime().toString());
        value.put("endTime", reservation.getEndTime().toString());
        value.put("bufferMinutes", reservation.getBufferMinutes());
        value.put("partySize", reservation.getPartySize());
        value.put("status", reservation.getStatus().name());
        value.put("rescheduleCount", reservation.getRescheduleCount());
        value.put("version", reservation.getVersion());

        value.put(
                "requestedAreaId",
                requestedArea == null ? null : requestedArea.getId()
        );

        return value;
    }

    @Transactional(
            readOnly = true,
            isolation = Isolation.REPEATABLE_READ
    )
    public StaffReservationTimelineResponse getTimeline(
            LocalDate date,
            Long areaId
    ) {
        validateRequest(date, areaId);

        StaffReservationTimelineResponse.OpeningHours openingHours = loadOpeningHours(date);

        int gridStepMinutes = settingsService
                .load()
                .slotStepMinutes();

        // Biên ngày được tính theo giờ Việt Nam, không theo timezone server.
        Instant dayStart = date
                .atStartOfDay(RESTAURANT_ZONE)
                .toInstant();

        Instant dayEnd = date
                .plusDays(1)
                .atStartOfDay(RESTAURANT_ZONE)
                .toInstant();

        // Lấy bàn làm gốc để giữ cả những bàn chưa có đơn.
        List<RestaurantTable> tables = tableRepository
                .findForTimeline(areaId);

        Set<Long> tableIds = tables.stream()
                .map(RestaurantTable::getId)
                .collect(Collectors.toSet());

        Map<Long, List<StaffReservationTimelineResponse.ReservationItem>> reservationsByTable =
                loadReservations(tableIds, dayStart, dayEnd);

        Map<Long, List<StaffReservationTimelineResponse.SuspensionItem>> suspensionsByTable =
                loadSuspensions(tableIds, dayStart, dayEnd);

        List<StaffReservationTimelineResponse.TableItem> tableItems = tables.stream()
                .map(table -> toTableItem(
                        table,
                        reservationsByTable.getOrDefault(
                                table.getId(),
                                List.of()
                        ),
                        suspensionsByTable.getOrDefault(
                                table.getId(),
                                List.of()
                        )
                ))
                .toList();

        return new StaffReservationTimelineResponse(
                date,
                RESTAURANT_ZONE.getId(),
                openingHours,
                gridStepMinutes,
                tableItems
        );
    }

    private void validateRequest(LocalDate date, Long areaId) {
        if (date == null) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "DATE_REQUIRED",
                    "Ngày xem lịch là bắt buộc"
            );
        }

        if (areaId == null) {
            return;
        }

        if (areaId <= 0) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_AREA_ID",
                    "Mã khu vực phải lớn hơn 0"
            );
        }

        // Cho phép xem cả khu vực ngừng hoạt động để tra cứu lịch.
        if (!areaRepository.existsById(areaId)) {
            throw new BusinessException(
                    HttpStatus.NOT_FOUND,
                    "AREA_NOT_FOUND",
                    "Khu vực không tồn tại"
            );
        }
    }

    private StaffReservationTimelineResponse.OpeningHours loadOpeningHours(LocalDate date) {
        Optional<SpecialClosedDay> specialClosedDay =
                specialClosedDayRepository.findByClosedDate(date);

        if (specialClosedDay.isPresent()) {
            String reason = specialClosedDay.get().getReason();

            return new StaffReservationTimelineResponse.OpeningHours(
                    true,
                    null,
                    null,
                    reason == null || reason.isBlank()
                            ? "Nhà hàng đóng cửa vào ngày đã chọn"
                            : reason
            );
        }

        // Database: Chủ nhật = 0, thứ hai = 1, ..., thứ bảy = 6.
        short dayOfWeek =
                (short) (date.getDayOfWeek().getValue() % 7);

        OpeningHour hour = openingHourRepository
                .findById(dayOfWeek)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "OPENING_HOUR_NOT_CONFIGURED",
                        "Chưa cấu hình giờ mở cửa cho ngày này"
                ));

        if (hour.isClosed()) {
            return new StaffReservationTimelineResponse.OpeningHours(
                    true,
                    null,
                    null,
                    "Nhà hàng nghỉ theo lịch hằng tuần"
            );
        }

        if (hour.getOpenTime() == null
                || hour.getCloseTime() == null
                || !hour.getOpenTime().isBefore(hour.getCloseTime())) {
            throw new BusinessException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "INVALID_OPENING_HOUR_CONFIG",
                    "Cấu hình giờ mở cửa không hợp lệ"
            );
        }

        return new StaffReservationTimelineResponse.OpeningHours(
                false,
                hour.getOpenTime(),
                hour.getCloseTime(),
                null
        );
    }

    private Map<Long, List<StaffReservationTimelineResponse.ReservationItem>> loadReservations(
            Set<Long> tableIds,
            Instant dayStart,
            Instant dayEnd
    ) {
        if (tableIds.isEmpty()) {
            return Map.of();
        }

        // Truy vấn có sẵn xét occupied_range, bao gồm buffer.
        // Các trạng thái: PENDING, CONFIRMED, ARRIVED, COMPLETED.
        List<Long> reservationIds = reservationRepository
                .findTableOccupancyInRange(dayStart, dayEnd)
                .stream()
                .filter(reservation -> tableIds.contains(
                        reservation.getTable().getId()
                ))
                .map(Reservation::getId)
                .toList();

        // Không gửi IN () rỗng xuống database.
        if (reservationIds.isEmpty()) {
            return Map.of();
        }

        List<Reservation> reservations = reservationRepository
                .findTimelineDetailsByIds(reservationIds);

        Map<Long, List<StaffReservationTimelineResponse.ReservationItem>> result = new HashMap<>();

        for (Reservation reservation : reservations) {
            Long tableId = reservation.getTable().getId();

            result.computeIfAbsent(
                    tableId,
                    ignored -> new ArrayList<>()
            ).add(toReservationItem(reservation));
        }

        return result;
    }

    private Map<Long, List<StaffReservationTimelineResponse.SuspensionItem>> loadSuspensions(
            Set<Long> tableIds,
            Instant dayStart,
            Instant dayEnd
    ) {
        if (tableIds.isEmpty()) {
            return Map.of();
        }

        List<TableSuspension> suspensions = suspensionRepository
                .findAllOverlapping(dayStart, dayEnd)
                .stream()
                .filter(suspension -> tableIds.contains(
                        suspension.getTable().getId()
                ))
                .sorted(
                        Comparator.comparing(TableSuspension::getStartTime)
                                .thenComparing(TableSuspension::getId)
                )
                .toList();

        Map<Long, List<StaffReservationTimelineResponse.SuspensionItem>> result = new HashMap<>();

        for (TableSuspension suspension : suspensions) {
            Long tableId = suspension.getTable().getId();

            StaffReservationTimelineResponse.SuspensionItem item = new StaffReservationTimelineResponse.SuspensionItem(
                    suspension.getId(),
                    suspension.getStartTime(),
                    suspension.getEndTime(),
                    suspension.getReason()
            );

            result.computeIfAbsent(
                    tableId,
                    ignored -> new ArrayList<>()
            ).add(item);
        }

        return result;
    }

    private StaffReservationTimelineResponse.ReservationItem toReservationItem(
            Reservation reservation
    ) {
        String customerName = reservation.getCustomer() == null
                ? reservation.getGuestName()
                : reservation.getCustomer().getFullName();

        // Dùng buffer đã lưu trên đơn, không dùng cấu hình hiện tại.
        Instant occupiedUntil = reservation
                .getEndTime()
                .plus(
                        reservation.getBufferMinutes(),
                        ChronoUnit.MINUTES
                );

        return new StaffReservationTimelineResponse.ReservationItem(
                reservation.getId(),
                reservation.getCode(),
                customerName,
                reservation.getPartySize(),
                reservation.getStatus(),
                reservation.getStartTime(),
                reservation.getEndTime(),
                reservation.getBufferMinutes(),
                occupiedUntil
        );
    }

    private StaffReservationTimelineResponse.TableItem toTableItem(
            RestaurantTable table,
            List<StaffReservationTimelineResponse.ReservationItem> reservations,
            List<StaffReservationTimelineResponse.SuspensionItem> suspensions
    ) {
        Area area = table.getArea();

        return new StaffReservationTimelineResponse.TableItem(
                table.getId(),
                table.getCode(),
                table.isActive(),
                new StaffReservationTimelineResponse.AreaItem(
                        area.getId(),
                        area.getName(),
                        area.isActive()
                ),
                table.getMinCapacity(),
                table.getMaxCapacity(),
                List.copyOf(reservations),
                List.copyOf(suspensions)
        );
    }

    private AreaSummary toMoveAreaSummary(Area area) {
        return area == null
                ? null
                : new AreaSummary(area.getId(), area.getName());
    }

    private TableOption toMoveTableOption(
            MoveTableOptionRow row,
            int partySize,
            Long requestedAreaId
    ) {
        Availability availability;
        String unavailableReason = null;
        BlockingPeriod blockingPeriod = null;

        // Ưu tiên một lý do chính khi bàn có nhiều điều kiện không phù hợp.
        if (!row.getActive()) {
            availability = Availability.INACTIVE;
            unavailableReason = "Bàn đã ngừng hoạt động";

        } else if (partySize < row.getMinCapacity()
                || partySize > row.getMaxCapacity()) {
            availability = Availability.CAPACITY_MISMATCH;

            unavailableReason = partySize > row.getMaxCapacity()
                    ? "Không đủ chỗ cho " + partySize + " khách"
                    : "Bàn nhận tối thiểu " + row.getMinCapacity() + " khách";

        } else if (row.getSuspendedStart() != null) {
            availability = Availability.SUSPENDED;

            String reason = row.getSuspensionReason();

            unavailableReason = reason == null || reason.isBlank()
                    ? "Bàn tạm ngưng trong khoảng thời gian này"
                    : "Bàn tạm ngưng: " + reason.strip();

            blockingPeriod = new BlockingPeriod(
                    toDetailTime(row.getSuspendedStart()),
                    toDetailTime(row.getSuspendedEnd())
            );

        } else if (row.getBusyStart() != null) {
            availability = Availability.BUSY;
            unavailableReason = "Bàn đã được giữ chỗ trong khoảng thời gian này";

            // busyEnd là upper(occupied_range), đã tính cả buffer.
            blockingPeriod = new BlockingPeriod(
                    toDetailTime(row.getBusyStart()),
                    toDetailTime(row.getBusyEnd())
            );

        } else {
            availability = Availability.AVAILABLE;
        }

        boolean outsideRequestedArea =
                requestedAreaId != null
                        && !Objects.equals(
                        requestedAreaId,
                        row.getAreaId()
                );

        return new TableOption(
                row.getId(),
                row.getCode(),
                new AreaSummary(
                        row.getAreaId(),
                        row.getAreaName()
                ),
                row.getMinCapacity(),
                row.getMaxCapacity(),
                availability,
                availability == Availability.AVAILABLE,
                outsideRequestedArea,
                blockingPeriod,
                unavailableReason
        );
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