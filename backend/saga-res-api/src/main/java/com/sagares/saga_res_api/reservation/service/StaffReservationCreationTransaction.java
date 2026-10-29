package com.sagares.saga_res_api.reservation.service;

import com.sagares.saga_res_api.account.entity.Account;
import com.sagares.saga_res_api.account.entity.AccountRole;
import com.sagares.saga_res_api.account.repository.AccountRepository;
import com.sagares.saga_res_api.common.exception.BusinessException;
import com.sagares.saga_res_api.common.realtime.RealtimeChange;
import com.sagares.saga_res_api.common.realtime.RealtimeEvent;
import com.sagares.saga_res_api.reservation.dto.CreateGuestReservationRequest;
import com.sagares.saga_res_api.reservation.dto.CreateGuestReservationResponse;
import com.sagares.saga_res_api.reservation.entity.Reservation;
import com.sagares.saga_res_api.reservation.entity.ReservationLogEventType;
import com.sagares.saga_res_api.reservation.entity.ReservationStatus;
import com.sagares.saga_res_api.reservation.entity.ReservationStatusLog;
import com.sagares.saga_res_api.reservation.repository.ReservationRepository;
import com.sagares.saga_res_api.reservation.repository.ReservationStatusLogRepository;
import com.sagares.saga_res_api.restaurant.entity.OpeningHour;
import com.sagares.saga_res_api.restaurant.repository.OpeningHourRepository;
import com.sagares.saga_res_api.restaurant.repository.SpecialClosedDayRepository;
import com.sagares.saga_res_api.table.entity.RestaurantTable;
import com.sagares.saga_res_api.table.repository.AreaRepository;
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
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static com.sagares.saga_res_api.reservation.service
        .ReservationSettingsService.RESTAURANT_ZONE;

@Service
@RequiredArgsConstructor
public class StaffReservationCreationTransaction {

    private final AccountRepository accountRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationStatusLogRepository statusLogRepository;

    private final RestaurantTableRepository tableRepository;
    private final TableSuspensionRepository suspensionRepository;
    private final AreaRepository areaRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final OpeningHourRepository openingHourRepository;
    private final SpecialClosedDayRepository closedDayRepository;

    private final ReservationSettingsService settingsService;
    private final EntityManager entityManager;
    private final Clock clock;

    private record BookingWindow(
            Instant start,
            Instant end,
            Instant occupiedEnd
    ) {
    }

    @Transactional(
            propagation = Propagation.REQUIRES_NEW,
            isolation = Isolation.READ_COMMITTED
    )
    public CreateGuestReservationResponse create(
            Long staffId,
            CreateGuestReservationRequest request
    ) {
        Account staff = loadStaff(staffId);

        var settings = settingsService.load();
        BookingWindow window = validateRequest(request, settings);

        var candidateIds = tableRepository.findCandidateIds(
                request.partySize().shortValue(),
                request.areaId()
        );

        // Repository đã sắp xếp maxCapacity ASC, code ASC.
        for (Long tableId : candidateIds) {
            RestaurantTable table = tableRepository
                    .findByIdForUpdate(tableId)
                    .orElse(null);

            if (table == null) {
                continue;
            }

            // Đọc lại trạng thái bàn sau khi đã lấy khóa.
            entityManager.refresh(table);

            if (!isSuitable(table, request)) {
                continue;
            }

            boolean occupied = reservationRepository.existsTableOverlap(
                    tableId,
                    window.start(),
                    window.occupiedEnd()
            );

            boolean suspended = !suspensionRepository.findOverlapping(
                    tableId,
                    window.start(),
                    window.occupiedEnd()
            ).isEmpty();

            if (occupied || suspended) {
                continue;
            }

            // Request có thể đã mất thời gian chờ khóa.
            validateNotPast(window.start());

            Reservation reservation = buildReservation(
                    staff,
                    table,
                    request,
                    settings,
                    window
            );

            reservation = reservationRepository.saveAndFlush(reservation);

            saveCreatedLog(reservation, staff);

            eventPublisher.publishEvent(new RealtimeChange(
                    RealtimeChange.Kind.RESERVATION,
                    null,
                    RealtimeEvent.of(
                            "RESERVATION_CREATED",
                            reservation.getId()
                    )
            ));

            return new CreateGuestReservationResponse(
                    reservation.getCode(),
                    reservation.getGuestName(),
                    table.getCode(),
                    reservation.getPartySize().intValue(),
                    reservation.getStartTime()
                            .atZone(RESTAURANT_ZONE)
                            .toOffsetDateTime(),
                    reservation.getEndTime()
                            .atZone(RESTAURANT_ZONE)
                            .toOffsetDateTime()
            );
        }

        throw new BusinessException(
                HttpStatus.CONFLICT,
                "NO_AVAILABLE_TABLE",
                "Không còn bàn phù hợp trong khoảng thời gian đã chọn"
        );
    }

    private Account loadStaff(Long staffId) {
        Account staff = accountRepository.findByIdForUpdate(staffId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.UNAUTHORIZED,
                        "UNAUTHORIZED",
                        "Tài khoản không còn tồn tại"
                ));

        // JWT filter có thể đã tải Account vào persistence context.
        entityManager.refresh(staff);

        if (staff.isLocked()) {
            throw new BusinessException(
                    HttpStatus.UNAUTHORIZED,
                    "ACCOUNT_LOCKED",
                    "Tài khoản đã bị khóa"
            );
        }

        if (staff.getRole() != AccountRole.STAFF
                && staff.getRole() != AccountRole.ADMIN) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN,
                    "FORBIDDEN",
                    "Chức năng dành cho nhân viên hoặc quản trị viên"
            );
        }

        return staff;
    }

    private BookingWindow validateRequest(
            CreateGuestReservationRequest request,
            ReservationSettingsService.Settings settings
    ) {
        if (request.partySize() > settings.maxPartySize()) {
            throw invalid(
                    "INVALID_PARTY_SIZE",
                    "Số khách vượt quá giới hạn đặt bàn"
            );
        }

        if (!request.timeEnd().isAfter(request.timeStart())) {
            throw invalid(
                    "INVALID_TIME_RANGE",
                    "Giờ kết thúc phải sau giờ bắt đầu trong cùng ngày"
            );
        }

        if (!isOnTimeGrid(request.timeStart(), settings.slotStepMinutes())
                || !isOnTimeGrid(
                request.timeEnd(),
                settings.slotStepMinutes()
        )) {
            throw invalid(
                    "INVALID_TIME_STEP",
                    "Giờ đặt bàn phải theo bước "
                            + settings.slotStepMinutes()
                            + " phút, không có giây"
            );
        }

        LocalDate today = clock.instant()
                .atZone(RESTAURANT_ZONE)
                .toLocalDate();

        LocalDate lastDate = today.plusDays(settings.maxAdvanceDays());

        if (request.date().isBefore(today)
                || request.date().isAfter(lastDate)) {
            throw invalid(
                    "INVALID_RESERVATION_DATE",
                    "Ngày đặt phải từ hôm nay đến " + lastDate
            );
        }

        Instant start = request.date()
                .atTime(request.timeStart())
                .atZone(RESTAURANT_ZONE)
                .toInstant();

        Instant end = request.date()
                .atTime(request.timeEnd())
                .atZone(RESTAURANT_ZONE)
                .toInstant();

        validateNotPast(start);

        if (request.areaId() != null
                && !areaRepository.existsById(request.areaId())) {
            throw invalid(
                    "INVALID_AREA",
                    "Khu vực được chọn không tồn tại"
            );
        }

        if (closedDayRepository.existsByClosedDate(request.date())) {
            throw invalid(
                    "RESTAURANT_CLOSED",
                    "Nhà hàng đóng cửa vào ngày đã chọn"
            );
        }

        // Database: Chủ nhật = 0, thứ Hai = 1, ... thứ Bảy = 6.
        short dayOfWeek =
                (short) (request.date().getDayOfWeek().getValue() % 7);

        OpeningHour openingHour = openingHourRepository
                .findById(dayOfWeek)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "RESERVATION_CONFIG_INVALID",
                        "Thiếu cấu hình giờ mở cửa"
                ));

        if (openingHour.isClosed()) {
            throw invalid(
                    "RESTAURANT_CLOSED",
                    "Nhà hàng không phục vụ vào ngày đã chọn"
            );
        }

        if (openingHour.getOpenTime() == null
                || openingHour.getCloseTime() == null
                || !openingHour.getOpenTime()
                .isBefore(openingHour.getCloseTime())) {
            throw new BusinessException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "RESERVATION_CONFIG_INVALID",
                    "Cấu hình giờ mở cửa không hợp lệ"
            );
        }

        if (request.timeStart().isBefore(openingHour.getOpenTime())
                || request.timeEnd().isAfter(openingHour.getCloseTime())) {
            throw invalid(
                    "OUTSIDE_OPENING_HOURS",
                    "Khoảng thời gian đặt nằm ngoài giờ phục vụ"
            );
        }

        // Thời lượng do nhân viên chọn tự do trong giờ mở cửa.
        // Buffer vẫn được cộng vào khoảng chiếm bàn.
        Instant occupiedEnd = end.plus(
                settings.bufferMinutes(),
                ChronoUnit.MINUTES
        );

        return new BookingWindow(start, end, occupiedEnd);
    }

    private boolean isOnTimeGrid(LocalTime time, int stepMinutes) {
        return time.getSecond() == 0
                && time.getNano() == 0
                && time.getMinute() % stepMinutes == 0;
    }

    private void validateNotPast(Instant start) {
        Instant currentMinute =
                clock.instant().truncatedTo(ChronoUnit.MINUTES);

        if (start.isBefore(currentMinute)) {
            throw invalid(
                    "RESERVATION_TIME_IN_PAST",
                    "Giờ bắt đầu không được thuộc phút đã qua"
            );
        }
    }

    private boolean isSuitable(
            RestaurantTable table,
            CreateGuestReservationRequest request
    ) {
        return table.isActive()
                && table.getMinCapacity() <= request.partySize()
                && table.getMaxCapacity() >= request.partySize()
                && (
                request.areaId() == null
                        || request.areaId().equals(table.getArea().getId())
        );
    }

    private Reservation buildReservation(
            Account staff,
            RestaurantTable table,
            CreateGuestReservationRequest request,
            ReservationSettingsService.Settings settings,
            BookingWindow window
    ) {
        Reservation reservation = new Reservation();

        reservation.setCode(generateCode(request.date()));

        reservation.setCustomer(null);
        reservation.setGuestName(request.name());
        reservation.setGuestPhone(request.phone());
        reservation.setCreatedBy(staff);

        reservation.setTable(table);
        reservation.setRequestedArea(
                request.areaId() == null ? null : table.getArea()
        );

        reservation.setPartySize(request.partySize().shortValue());
        reservation.setStartTime(window.start());
        reservation.setEndTime(window.end());
        reservation.setBufferMinutes((short) settings.bufferMinutes());

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setSpecialNote(request.description());
        reservation.setRescheduleCount((short) 0);

        return reservation;
    }

    private void saveCreatedLog(
            Reservation reservation,
            Account staff
    ) {
        // LinkedHashMap cho phép các giá trị null.
        Map<String, Object> newValue = new LinkedHashMap<>();

        newValue.put("code", reservation.getCode());
        newValue.put("customerId", null);
        newValue.put("guestName", reservation.getGuestName());
        newValue.put("guestPhone", reservation.getGuestPhone());
        newValue.put("createdBy", staff.getId());

        newValue.put("tableId", reservation.getTable().getId());
        newValue.put(
                "requestedAreaId",
                reservation.getRequestedArea() == null
                        ? null
                        : reservation.getRequestedArea().getId()
        );

        newValue.put("partySize", reservation.getPartySize());
        newValue.put("startTime", reservation.getStartTime().toString());
        newValue.put("endTime", reservation.getEndTime().toString());
        newValue.put("bufferMinutes", reservation.getBufferMinutes());
        newValue.put("status", reservation.getStatus().name());
        newValue.put("specialNote", reservation.getSpecialNote());
        newValue.put("rescheduleCount", reservation.getRescheduleCount());

        ReservationStatusLog log = new ReservationStatusLog();
        log.setReservation(reservation);
        log.setEventType(ReservationLogEventType.CREATED);
        log.setOldValue(null);
        log.setNewValue(newValue);
        log.setChangedBy(staff);
        log.setChangedAt(clock.instant());

        // Lỗi lưu log sẽ rollback cả reservation.
        statusLogRepository.saveAndFlush(log);
    }

    private String generateCode(LocalDate date) {
        String datePart =
                date.format(DateTimeFormatter.ofPattern("yyMMdd"));

        String randomPart = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 8)
                .toUpperCase(Locale.ROOT);

        return "SAGE-" + datePart + "-" + randomPart;
    }

    private BusinessException invalid(String code, String message) {
        return new BusinessException(HttpStatus.BAD_REQUEST, code, message);
    }
}