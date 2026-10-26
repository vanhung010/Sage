package com.sagares.saga_res_api.reservation.service;

import com.sagares.saga_res_api.account.entity.Account;
import com.sagares.saga_res_api.account.entity.AccountRole;
import com.sagares.saga_res_api.account.repository.AccountRepository;
import com.sagares.saga_res_api.common.exception.BusinessException;
import com.sagares.saga_res_api.reservation.dto.StaffReservationDurationDto.*;
import com.sagares.saga_res_api.reservation.entity.Reservation;
import com.sagares.saga_res_api.reservation.entity.ReservationLogEventType;
import com.sagares.saga_res_api.reservation.entity.ReservationStatus;
import com.sagares.saga_res_api.reservation.entity.ReservationStatusLog;
import com.sagares.saga_res_api.reservation.repository.ReservationRepository;
import com.sagares.saga_res_api.reservation.repository.ReservationStatusLogRepository;
import com.sagares.saga_res_api.restaurant.repository.OpeningHourRepository;
import com.sagares.saga_res_api.restaurant.repository.SpecialClosedDayRepository;
import com.sagares.saga_res_api.table.repository.RestaurantTableRepository;
import com.sagares.saga_res_api.table.repository.TableSuspensionRepository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
public class StaffReservationDurationTransaction {

    private static final ZoneId RESTAURANT_ZONE =
            ZoneId.of("Asia/Ho_Chi_Minh");

    private final ReservationRepository reservationRepository;
    private final ReservationStatusLogRepository statusLogRepository;
    private final AccountRepository accountRepository;
    private final RestaurantTableRepository tableRepository;
    private final TableSuspensionRepository suspensionRepository;
    private final OpeningHourRepository openingHourRepository;
    private final SpecialClosedDayRepository closedDayRepository;
    private final ReservationSettingsService settingsService;
    private final EntityManager entityManager;
    private final Clock clock;

    @Transactional(
            readOnly = true,
            propagation = Propagation.REQUIRES_NEW,
            isolation = Isolation.REPEATABLE_READ
    )
    public PreviewResponse preview(Long id, PreviewRequest request) {
        Reservation reservation = findReservation(id);

        // Đọc lại entity nếu persistence context đã có bản được tải trước đó.
        entityManager.refresh(reservation);

        return inspect(reservation, request.endTime().toInstant());
    }

    @Transactional(
            propagation = Propagation.REQUIRES_NEW,
            isolation = Isolation.READ_COMMITTED
    )
    public ChangeResponse change(
            Long staffId,
            Long id,
            ChangeRequest request
    ) {
        Reservation initial = findReservation(id);
        Long initialCustomerId = customerId(initial);

        // Giữ thứ tự khóa tương thích với luồng khách đổi lịch:
        // customer account -> reservation -> restaurant table.
        // Đơn khách vãng lai không có customer account.
        if (initialCustomerId != null) {
            accountRepository.findByIdForUpdate(initialCustomerId)
                    .orElseThrow(() -> error(
                            HttpStatus.CONFLICT,
                            "RESERVATION_CHANGED",
                            "Thông tin khách đã thay đổi, vui lòng tải lại đơn"
                    ));
        }

        Reservation reservation = reservationRepository
                .findByIdForUpdate(id)
                .orElseThrow(() -> error(
                        HttpStatus.NOT_FOUND,
                        "RESERVATION_NOT_FOUND",
                        "Không tìm thấy đơn đặt bàn"
                ));

        // findByIdForUpdate có thể trả entity đã nằm trong persistence context.
        // Refresh sau khi lấy khóa để kiểm tra dữ liệu mới nhất.
        entityManager.refresh(reservation);

        if (!Objects.equals(
                initialCustomerId,
                customerId(reservation)
        ) || !Objects.equals(
                request.expectedVersion(),
                reservation.getVersion()
        )) {
            throw error(
                    HttpStatus.CONFLICT,
                    "RESERVATION_VERSION_CONFLICT",
                    "Đơn đặt bàn đã được cập nhật, vui lòng tải lại"
            );
        }

        // Cùng khóa bàn với luồng đặt bàn và tạo lịch tạm ngưng bàn.
        tableRepository.findByIdForUpdate(reservation.getTable().getId())
                .orElseThrow(() -> error(
                        HttpStatus.CONFLICT,
                        "RESERVATION_TABLE_NOT_FOUND",
                        "Bàn của đơn đặt không còn tồn tại"
                ));

        Account actor = loadActor(staffId);
        Instant newEnd = request.endTime().toInstant();

        // Preview trước đó không giữ chỗ. Luôn kiểm tra lại sau khi khóa.
        PreviewResponse checked = inspect(reservation, newEnd);

        if (!checked.allowed()) {
            Conflict conflict = checked.conflicts().getFirst();

            throw error(
                    HttpStatus.CONFLICT,
                    conflict.code(),
                    conflict.message()
            );
        }

        // Không thay đổi dữ liệu và không ghi log khi giờ kết thúc giữ nguyên.
        if (newEnd.equals(reservation.getEndTime())) {
            return toChangeResponse(reservation);
        }

        Map<String, Object> oldValue = snapshot(reservation);

        // Chỉ cập nhật endTime.
        // Trigger database tự cập nhật reservation_range và occupied_range.
        reservation.setEndTime(newEnd);
        reservationRepository.saveAndFlush(reservation);

        ReservationStatusLog log = new ReservationStatusLog();
        log.setReservation(reservation);
        log.setEventType(ReservationLogEventType.TIME_CHANGED);
        log.setOldValue(oldValue);
        log.setNewValue(snapshot(reservation));
        log.setChangedBy(actor);
        log.setChangedAt(clock.instant());
        log.setReason(normalizeReason(request.reason()));

        // Log và reservation cùng commit hoặc cùng rollback.
        statusLogRepository.saveAndFlush(log);

        return toChangeResponse(reservation);
    }

    private PreviewResponse inspect(
            Reservation reservation,
            Instant newEnd
    ) {
        validateStatus(reservation);

        if (newEnd.equals(reservation.getEndTime())) {
            return toPreviewResponse(
                    reservation,
                    newEnd,
                    List.of(),
                    null
            );
        }

        int stepMinutes = validateEndTime(reservation, newEnd);
        Instant start = reservation.getStartTime();
        int buffer = reservation.getBufferMinutes();
        Instant occupiedEnd = newEnd.plus(buffer, ChronoUnit.MINUTES);

        List<Conflict> conflicts = new ArrayList<>();
        Instant latestPossibleEnd = newEnd;

        var reservations = reservationRepository.findDurationConflicts(
                reservation.getId(),
                reservation.getTable().getId(),
                customerId(reservation),
                start,
                newEnd,
                occupiedEnd
        );

        for (var other : reservations) {
            boolean sameTable = Objects.equals(
                    reservation.getTable().getId(),
                    other.getTableId()
            );

            String code = sameTable
                    ? "TABLE_TIME_CONFLICT"
                    : "CUSTOMER_TIME_CONFLICT";

            String message = sameTable
                    ? "Thời gian chiếm bàn trùng với đơn " + other.getCode()
                    : "Khách đã có đơn trùng giờ: " + other.getCode();

            Instant blockedUntil = sameTable
                    ? other.getEndTime().plus(
                    other.getBufferMinutes(),
                    ChronoUnit.MINUTES
            )
                    : other.getEndTime();

            conflicts.add(new Conflict(
                    code,
                    message,
                    toTime(other.getStartTime()),
                    toTime(blockedUntil)
            ));

            // Với bàn, phải chừa buffer của đơn đang được chỉnh sửa.
            // Với khách, hai khoảng dùng bữa được phép nối tiếp nhau.
            Instant limit = sameTable
                    ? other.getStartTime().minus(buffer, ChronoUnit.MINUTES)
                    : other.getStartTime();

            latestPossibleEnd = earlier(latestPossibleEnd, limit);
        }

        var suspensions = suspensionRepository.findOverlapping(
                reservation.getTable().getId(),
                start,
                occupiedEnd
        );

        for (var suspension : suspensions) {
            conflicts.add(new Conflict(
                    "TABLE_SUSPENDED",
                    "Bàn bị tạm ngưng trong khoảng thời gian này",
                    toTime(suspension.getStartTime()),
                    toTime(suspension.getEndTime())
            ));

            Instant limit = suspension.getStartTime()
                    .minus(buffer, ChronoUnit.MINUTES);

            latestPossibleEnd = earlier(latestPossibleEnd, limit);
        }

        OffsetDateTime suggestedEnd = null;

        if (!conflicts.isEmpty()) {
            Instant candidate = floorToStep(latestPossibleEnd, stepMinutes);

            if (candidate.isAfter(start)
                    && candidate.isAfter(clock.instant())) {
                suggestedEnd = toTime(candidate);
            }
        }

        return toPreviewResponse(
                reservation,
                newEnd,
                conflicts,
                suggestedEnd
        );
    }

    private int validateEndTime(
            Reservation reservation,
            Instant newEnd
    ) {
        var start = reservation.getStartTime().atZone(RESTAURANT_ZONE);
        var end = newEnd.atZone(RESTAURANT_ZONE);

        if (!newEnd.isAfter(reservation.getStartTime())) {
            throw error(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_END_TIME",
                    "Giờ kết thúc phải sau giờ bắt đầu"
            );
        }

        if (!start.toLocalDate().equals(end.toLocalDate())) {
            throw error(
                    HttpStatus.BAD_REQUEST,
                    "RESERVATION_DATE_MISMATCH",
                    "Giờ kết thúc phải cùng ngày với giờ bắt đầu"
            );
        }

        if (!newEnd.isAfter(clock.instant())) {
            throw error(
                    HttpStatus.BAD_REQUEST,
                    "END_TIME_IN_PAST",
                    "Giờ kết thúc mới phải sau thời điểm hiện tại"
            );
        }

        int step = settingsService.load().slotStepMinutes();
        int minuteOfDay = end.getHour() * 60 + end.getMinute();

        if (end.getSecond() != 0
                || end.getNano() != 0
                || minuteOfDay % step != 0) {
            throw error(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_TIME_STEP",
                    "Giờ kết thúc phải theo bước " + step + " phút"
            );
        }

        var date = start.toLocalDate();

        if (closedDayRepository.existsByClosedDate(date)) {
            throw error(
                    HttpStatus.BAD_REQUEST,
                    "RESTAURANT_CLOSED",
                    "Nhà hàng đóng cửa trong ngày này"
            );
        }

        // Database: Chủ nhật = 0, Thứ hai = 1, ..., Thứ bảy = 6.
        short dayOfWeek = (short) (date.getDayOfWeek().getValue() % 7);

        var opening = openingHourRepository.findById(dayOfWeek)
                .orElseThrow(() -> error(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "RESERVATION_CONFIG_INVALID",
                        "Thiếu cấu hình giờ hoạt động"
                ));

        if (opening.isClosed()) {
            throw error(
                    HttpStatus.BAD_REQUEST,
                    "RESTAURANT_CLOSED",
                    "Nhà hàng đóng cửa trong ngày này"
            );
        }

        if (opening.getOpenTime() == null
                || opening.getCloseTime() == null
                || !opening.getOpenTime().isBefore(opening.getCloseTime())) {
            throw error(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "RESERVATION_CONFIG_INVALID",
                    "Cấu hình giờ hoạt động không hợp lệ"
            );
        }

        if (start.toLocalTime().isBefore(opening.getOpenTime())
                || end.toLocalTime().isAfter(opening.getCloseTime())) {
            throw error(
                    HttpStatus.BAD_REQUEST,
                    "OUTSIDE_OPENING_HOURS",
                    "Thời gian dùng bàn phải nằm trong giờ hoạt động"
            );
        }

        // Staff không áp dụng giới hạn thời lượng 60–180 phút của khách.
        // Giờ kết thúc dùng bữa phải <= giờ đóng cửa.
        // Buffer vẫn được cộng riêng khi kiểm tra chiếm bàn.
        return step;
    }

    private void validateStatus(Reservation reservation) {
        if (reservation.getStatus() != ReservationStatus.CONFIRMED
                && reservation.getStatus() != ReservationStatus.ARRIVED) {
            throw error(
                    HttpStatus.CONFLICT,
                    "DURATION_CHANGE_NOT_ALLOWED",
                    "Chỉ được đổi thời lượng đơn đã xác nhận hoặc khách đã đến"
            );
        }
    }

    private Reservation findReservation(Long id) {
        if (id == null || id <= 0) {
            throw error(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_RESERVATION_ID",
                    "ID đơn đặt bàn phải lớn hơn 0"
            );
        }

        return reservationRepository.findById(id)
                .orElseThrow(() -> error(
                        HttpStatus.NOT_FOUND,
                        "RESERVATION_NOT_FOUND",
                        "Không tìm thấy đơn đặt bàn"
                ));
    }

    private Account loadActor(Long staffId) {
        Account actor = accountRepository.findById(staffId)
                .orElseThrow(() -> error(
                        HttpStatus.UNAUTHORIZED,
                        "ACCOUNT_NOT_FOUND",
                        "Tài khoản không còn tồn tại"
                ));

        entityManager.refresh(actor);

        if (actor.isLocked()) {
            throw error(
                    HttpStatus.UNAUTHORIZED,
                    "ACCOUNT_LOCKED",
                    "Tài khoản đã bị khóa"
            );
        }

        if (actor.getRole() != AccountRole.STAFF
                && actor.getRole() != AccountRole.ADMIN) {
            throw error(
                    HttpStatus.FORBIDDEN,
                    "ACCESS_DENIED",
                    "Bạn không có quyền đổi thời lượng đặt bàn"
            );
        }

        return actor;
    }

    private PreviewResponse toPreviewResponse(
            Reservation reservation,
            Instant newEnd,
            List<Conflict> conflicts,
            OffsetDateTime suggestedEnd
    ) {
        long currentDuration = Duration.between(
                reservation.getStartTime(),
                reservation.getEndTime()
        ).toMinutes();

        long newDuration = Duration.between(
                reservation.getStartTime(),
                newEnd
        ).toMinutes();

        return new PreviewResponse(
                reservation.getId(),
                reservation.getVersion(),
                reservation.getStatus(),
                toTime(reservation.getStartTime()),
                toTime(reservation.getEndTime()),
                toTime(newEnd),
                reservation.getBufferMinutes(),
                currentDuration,
                newDuration,
                newDuration - currentDuration,
                toTime(newEnd.plus(
                        reservation.getBufferMinutes(),
                        ChronoUnit.MINUTES
                )),
                conflicts.isEmpty(),
                List.copyOf(conflicts),
                suggestedEnd
        );
    }

    private ChangeResponse toChangeResponse(Reservation reservation) {
        return new ChangeResponse(
                reservation.getId(),
                reservation.getCode(),
                reservation.getVersion(),
                reservation.getStatus(),
                toTime(reservation.getStartTime()),
                toTime(reservation.getEndTime()),
                Duration.between(
                        reservation.getStartTime(),
                        reservation.getEndTime()
                ).toMinutes(),
                reservation.getBufferMinutes(),
                toTime(reservation.getEndTime().plus(
                        reservation.getBufferMinutes(),
                        ChronoUnit.MINUTES
                )),
                toTime(reservation.getUpdatedAt())
        );
    }

    private Map<String, Object> snapshot(Reservation reservation) {
        // LinkedHashMap cho phép requestedAreaId là null.
        Map<String, Object> value = new LinkedHashMap<>();

        value.put("startTime", reservation.getStartTime().toString());
        value.put("endTime", reservation.getEndTime().toString());
        value.put("status", reservation.getStatus().name());
        value.put("tableId", reservation.getTable().getId());
        value.put("partySize", reservation.getPartySize());
        value.put("bufferMinutes", reservation.getBufferMinutes());
        value.put("rescheduleCount", reservation.getRescheduleCount());
        value.put("version", reservation.getVersion());

        value.put(
                "requestedAreaId",
                reservation.getRequestedArea() == null
                        ? null
                        : reservation.getRequestedArea().getId()
        );

        return value;
    }

    private Long customerId(Reservation reservation) {
        return reservation.getCustomer() == null
                ? null
                : reservation.getCustomer().getId();
    }

    private Instant floorToStep(Instant value, int stepMinutes) {
        var local = value.atZone(RESTAURANT_ZONE);
        int minuteOfDay = local.getHour() * 60 + local.getMinute();
        int rounded = minuteOfDay - minuteOfDay % stepMinutes;

        return local.toLocalDate()
                .atStartOfDay(RESTAURANT_ZONE)
                .plusMinutes(rounded)
                .toInstant();
    }

    private Instant earlier(Instant first, Instant second) {
        return first.isBefore(second) ? first : second;
    }

    private OffsetDateTime toTime(Instant value) {
        return value == null
                ? null
                : value.atZone(RESTAURANT_ZONE).toOffsetDateTime();
    }

    private String normalizeReason(String value) {
        return value == null || value.isBlank()
                ? null
                : value.strip();
    }

    private BusinessException error(
            HttpStatus status,
            String code,
            String message
    ) {
        return new BusinessException(status, code, message);
    }
}