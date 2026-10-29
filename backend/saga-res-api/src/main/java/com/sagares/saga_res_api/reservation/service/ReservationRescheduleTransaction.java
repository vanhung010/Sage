package com.sagares.saga_res_api.reservation.service;

import com.sagares.saga_res_api.account.entity.Account;
import com.sagares.saga_res_api.account.entity.AccountRole;
import com.sagares.saga_res_api.account.repository.AccountRepository;
import com.sagares.saga_res_api.common.exception.BusinessException;
import com.sagares.saga_res_api.common.realtime.RealtimeChange;
import com.sagares.saga_res_api.common.realtime.RealtimeEvent;
import com.sagares.saga_res_api.reservation.dto.RescheduleReservationRequest;
import com.sagares.saga_res_api.reservation.dto.ReservationResponse;
import com.sagares.saga_res_api.reservation.entity.Reservation;
import com.sagares.saga_res_api.reservation.entity.ReservationLogEventType;
import com.sagares.saga_res_api.reservation.entity.ReservationStatus;
import com.sagares.saga_res_api.reservation.entity.ReservationStatusLog;
import com.sagares.saga_res_api.reservation.event.ReservationRescheduledEvent;
import com.sagares.saga_res_api.reservation.repository.ReservationRepository;
import com.sagares.saga_res_api.reservation.repository.ReservationStatusLogRepository;
import com.sagares.saga_res_api.table.entity.RestaurantTable;
import com.sagares.saga_res_api.table.repository.RestaurantTableRepository;
import com.sagares.saga_res_api.table.repository.TableSuspensionRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import static com.sagares.saga_res_api.reservation.service
        .ReservationSettingsService.RESTAURANT_ZONE;

@Service
@RequiredArgsConstructor
public class ReservationRescheduleTransaction {

    private final AccountRepository accounts;
    private final ReservationRepository reservations;
    private final RestaurantTableRepository tables;
    private final TableSuspensionRepository suspensions;
    private final ReservationStatusLogRepository logs;
    private final ReservationSettingsService settingsService;
    private final AvailabilityService availabilityService;
    private final EntityManager entityManager;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Transactional(
            propagation = Propagation.REQUIRES_NEW,
            isolation = Isolation.READ_COMMITTED
    )
    public ReservationResponse reschedule(
            Long customerId,
            Long id,
            RescheduleReservationRequest request
    ) {
        // Giữ cùng thứ tự lấy khóa với luồng tạo đơn.
        Account customer = accounts.findByIdForUpdate(customerId)
                .orElseThrow(() -> error(
                        HttpStatus.UNAUTHORIZED,
                        "ACCOUNT_NOT_FOUND",
                        "Tài khoản không tồn tại"
                ));

        entityManager.refresh(customer);

        if (customer.isLocked()) {
            throw error(
                    HttpStatus.UNAUTHORIZED,
                    "ACCOUNT_LOCKED",
                    "Tài khoản đã bị khóa"
            );
        }

        if (customer.getRole() != AccountRole.CUSTOMER) {
            throw error(
                    HttpStatus.FORBIDDEN,
                    "FORBIDDEN",
                    "Chức năng chỉ dành cho khách hàng"
            );
        }

        Reservation reservation = reservations
                .findOwnedByIdForUpdate(id, customerId)
                .orElseThrow(() -> error(
                        HttpStatus.NOT_FOUND,
                        "RESERVATION_NOT_FOUND",
                        "Không tìm thấy đơn đặt bàn"
                ));

        entityManager.refresh(reservation);

        var rules = settingsService.loadRescheduleSettings();
        validateEligibility(reservation, rules);

        var settings = settingsService.load();
        int duration = validateNewTime(request, settings);

        Instant start = request.startTime().toInstant();
        Instant end = request.endTime().toInstant();
        Instant occupiedEnd = end.plus(
                settings.bufferMinutes(),
                ChronoUnit.MINUTES
        );

        Long oldAreaId = reservation.getRequestedArea() == null
                ? null
                : reservation.getRequestedArea().getId();

        boolean unchanged =
                start.equals(reservation.getStartTime())
                        && end.equals(reservation.getEndTime())
                        && request.partySize().intValue()
                        == reservation.getPartySize().intValue()
                        && Objects.equals(request.requestedAreaId(), oldAreaId);

        if (unchanged) {
            throw error(
                    HttpStatus.BAD_REQUEST,
                    "RESERVATION_UNCHANGED",
                    "Bạn chưa thay đổi thông tin đặt bàn"
            );
        }

        if (reservations.existsCustomerOverlapExcludingReservation(
                id, customerId, start, end
        )) {
            throw error(
                    HttpStatus.CONFLICT,
                    "CUSTOMER_TIME_CONFLICT",
                    "Bạn đã có đơn trùng lịch mới"
            );
        }

        // Dùng lại quy tắc ngày, giờ mở cửa và tính giờ trống.
        var availability = availabilityService.searchForReschedule(
                request.startTime()
                        .atZoneSameInstant(RESTAURANT_ZONE)
                        .toLocalDate(),
                request.partySize(),
                duration,
                request.requestedAreaId(),
                reservation,
                clock.instant()
        );

        boolean available = availability.slots().stream()
                .anyMatch(slot ->
                        slot.startTime().toInstant().equals(start)
                                && slot.endTime().toInstant().equals(end)
                );

        if (!available) {
            throw error(
                    HttpStatus.CONFLICT,
                    "NO_AVAILABLE_TABLE",
                    "Khung giờ không còn bàn phù hợp hoặc nằm ngoài giờ phục vụ"
            );
        }

        var candidates = new ArrayList<>(tables.findCandidateIds(
                request.partySize().shortValue(),
                request.requestedAreaId()
        ));

        // Ưu tiên bàn hiện tại nếu bàn vẫn nằm trong danh sách phù hợp.
        Long currentTableId = reservation.getTable().getId();

        if (candidates.remove(currentTableId)) {
            candidates.add(0, currentTableId);
        }

        for (Long tableId : candidates) {
            RestaurantTable table = tables.findByIdForUpdate(tableId)
                    .orElse(null);

            if (table == null) {
                continue;
            }

            entityManager.refresh(table);

            if (!suitable(table, request)) {
                continue;
            }

            // Phải kiểm tra lại sau khi lấy khóa bàn.
            boolean occupied =
                    reservations.existsTableOverlapExcludingReservation(
                            id, tableId, start, occupiedEnd
                    );

            boolean suspended = !suspensions.findOverlapping(
                    tableId, start, occupiedEnd
            ).isEmpty();

            if (occupied || suspended) {
                continue;
            }

            // Request có thể vừa chờ khóa khá lâu.
            validateEligibility(reservation, rules);
            validateAdvance(start, request.partySize(), settings);

            Map<String, Object> before = snapshot(reservation);

            reservation.setTable(table);

            reservation.setRequestedArea(
                    request.requestedAreaId() == null
                            ? null
                            : table.getArea()
            );

            reservation.setStartTime(start);
            reservation.setEndTime(end);
            reservation.setPartySize(request.partySize().shortValue());
            reservation.setBufferMinutes((short) settings.bufferMinutes());

            reservation.setRescheduleCount(
                    (short) (reservation.getRescheduleCount() + 1)
            );

            reservation.setStatus(
                    request.partySize() <= settings.autoConfirmThreshold()
                            ? ReservationStatus.CONFIRMED
                            : ReservationStatus.PENDING
            );

            reservation.setReminderSentAt(null);

            // Giữ nguyên id, code, customer, specialNote, createdAt.
            reservations.saveAndFlush(reservation);

            ReservationStatusLog log = new ReservationStatusLog();
            log.setReservation(reservation);
            log.setEventType(ReservationLogEventType.RESCHEDULED);
            log.setOldValue(before);
            log.setNewValue(snapshot(reservation));
            log.setChangedBy(customer);
            log.setChangedAt(clock.instant());

            logs.saveAndFlush(log);

            ReservationResponse response =
                    ReservationService.toDTO(reservation);

            events.publishEvent(new ReservationRescheduledEvent(
                    customer.getEmail(),
                    response
            ));

            events.publishEvent(new RealtimeChange(
                    RealtimeChange.Kind.RESERVATION,
                    customer.getId(),
                    RealtimeEvent.of(
                            "RESERVATION_UPDATED",
                            reservation.getId()
                    )
            ));

            return response;
        }

        throw error(
                HttpStatus.CONFLICT,
                "NO_AVAILABLE_TABLE",
                "Không còn bàn phù hợp trong lịch mới"
        );
    }

    private void validateEligibility(
            Reservation reservation,
            ReservationSettingsService.RescheduleSettings rules
    ) {
        if (reservation.getStatus() != ReservationStatus.PENDING
                && reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw error(
                    HttpStatus.CONFLICT,
                    "RESERVATION_NOT_RESCHEDULABLE",
                    "Trạng thái không cho phép đổi lịch"
            );
        }

        if (reservation.getRescheduleCount()
                >= rules.maxRescheduleCount()) {
            throw error(
                    HttpStatus.CONFLICT,
                    "RESCHEDULE_LIMIT_REACHED",
                    "Đơn đã hết số lần đổi lịch"
            );
        }

        // Đúng mốc 2 giờ vẫn được đổi.
        // Ví dụ lịch cũ 17:00: được đổi đến hết mốc 15:00.
        if (clock.instant()
                .plus(rules.deadlineHours(), ChronoUnit.HOURS)
                .isAfter(reservation.getStartTime())) {
            throw error(
                    HttpStatus.CONFLICT,
                    "RESCHEDULE_DEADLINE_PASSED",
                    "Đã quá hạn đổi lịch của đơn hiện tại"
            );
        }
    }

    private int validateNewTime(
            RescheduleReservationRequest request,
            ReservationSettingsService.Settings settings
    ) {
        if (request == null
                || request.startTime() == null
                || request.endTime() == null
                || request.partySize() == null
                || request.partySize() < 1
                || request.partySize() > settings.maxPartySize()
                || (request.requestedAreaId() != null
                && request.requestedAreaId() <= 0)) {
            throw error(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_REQUEST",
                    "Thông tin lịch mới không hợp lệ"
            );
        }

        var start = request.startTime()
                .atZoneSameInstant(RESTAURANT_ZONE);

        var end = request.endTime()
                .atZoneSameInstant(RESTAURANT_ZONE);

        if (!end.isAfter(start)
                || !start.toLocalDate().equals(end.toLocalDate())) {
            throw error(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_TIME_RANGE",
                    "Giờ kết thúc phải sau giờ bắt đầu và cùng ngày"
            );
        }

        if (start.getSecond() != 0
                || start.getNano() != 0
                || end.getSecond() != 0
                || end.getNano() != 0
                || start.getMinute() % settings.slotStepMinutes() != 0
                || end.getMinute() % settings.slotStepMinutes() != 0) {
            throw error(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_TIME_STEP",
                    "Giờ hẹn phải theo bước 15 phút, không có giây"
            );
        }

        long minutes = Duration.between(start, end).toMinutes();

        if (minutes < settings.minDurationMinutes()
                || minutes > settings.maxDurationMinutes()) {
            throw error(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_DURATION",
                    "Thời lượng lịch mới không hợp lệ"
            );
        }

        validateAdvance(
                start.toInstant(),
                request.partySize(),
                settings
        );

        return (int) minutes;
    }

    private void validateAdvance(
            Instant start,
            int partySize,
            ReservationSettingsService.Settings settings
    ) {
        int hours = partySize > settings.autoConfirmThreshold()
                ? settings.minAdvanceHoursLargeGroup()
                : settings.minAdvanceHours();

        if (start.isBefore(
                clock.instant().plus(hours, ChronoUnit.HOURS)
        )) {
            throw error(
                    HttpStatus.BAD_REQUEST,
                    "INSUFFICIENT_ADVANCE_TIME",
                    "Lịch mới phải đặt trước ít nhất " + hours + " giờ"
            );
        }
    }

    private boolean suitable(
            RestaurantTable table,
            RescheduleReservationRequest request
    ) {
        return table.isActive()
                && table.getMinCapacity() <= request.partySize()
                && table.getMaxCapacity() >= request.partySize()
                && (
                request.requestedAreaId() == null
                        || request.requestedAreaId()
                        .equals(table.getArea().getId())
        );
    }

    private Map<String, Object> snapshot(Reservation reservation) {
        // Dùng LinkedHashMap vì requestedAreaId có thể null.
        Map<String, Object> value = new LinkedHashMap<>();

        value.put("startTime", reservation.getStartTime().toString());
        value.put("endTime", reservation.getEndTime().toString());
        value.put("partySize", reservation.getPartySize());
        value.put("tableId", reservation.getTable().getId());

        value.put(
                "requestedAreaId",
                reservation.getRequestedArea() == null
                        ? null
                        : reservation.getRequestedArea().getId()
        );

        value.put("status", reservation.getStatus().name());
        value.put("bufferMinutes", reservation.getBufferMinutes());
        value.put("rescheduleCount", reservation.getRescheduleCount());

        return value;
    }

    private BusinessException error(
            HttpStatus status,
            String code,
            String message
    ) {
        return new BusinessException(status, code, message);
    }
}