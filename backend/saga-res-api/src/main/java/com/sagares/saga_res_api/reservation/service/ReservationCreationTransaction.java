package com.sagares.saga_res_api.reservation.service;

import com.sagares.saga_res_api.account.entity.Account;
import com.sagares.saga_res_api.account.entity.AccountRole;
import com.sagares.saga_res_api.account.repository.AccountRepository;
import com.sagares.saga_res_api.common.exception.BusinessException;
import com.sagares.saga_res_api.reservation.dto.CreateReservationRequest;
import com.sagares.saga_res_api.reservation.dto.ReservationResponse;
import com.sagares.saga_res_api.reservation.entity.Reservation;
import com.sagares.saga_res_api.reservation.entity.ReservationLogEventType;
import com.sagares.saga_res_api.reservation.entity.ReservationStatus;
import com.sagares.saga_res_api.reservation.entity.ReservationStatusLog;
import com.sagares.saga_res_api.reservation.event.ReservationConfirmedEvent;
import com.sagares.saga_res_api.reservation.repository.ReservationRepository;
import com.sagares.saga_res_api.reservation.repository.ReservationStatusLogRepository;
import com.sagares.saga_res_api.restaurant.entity.OpeningHour;
import com.sagares.saga_res_api.restaurant.repository.OpeningHourRepository;
import com.sagares.saga_res_api.restaurant.repository.SpecialClosedDayRepository;
import com.sagares.saga_res_api.table.entity.Area;
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
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static com.sagares.saga_res_api.reservation.service
        .ReservationSettingsService.RESTAURANT_ZONE;

@Service
@RequiredArgsConstructor
public class ReservationCreationTransaction {

    private final AccountRepository accountRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationStatusLogRepository statusLogRepository;

    private final RestaurantTableRepository tableRepository;
    private final TableSuspensionRepository suspensionRepository;
    private final AreaRepository areaRepository;

    private final OpeningHourRepository openingHourRepository;
    private final SpecialClosedDayRepository closedDayRepository;

    private final ReservationSettingsService settingsService;
    private final ApplicationEventPublisher eventPublisher;
    private final EntityManager entityManager;
    private final Clock clock;

    private record BookingWindow(
            Instant start,
            Instant end,
            Instant occupiedEnd,
            LocalDate date,
            int advanceHours
    ) {
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public ReservationResponse create(
            Long customerId,
            CreateReservationRequest request
    ) {
        // Khóa tài khoản trước khi đếm đơn.
        //
        // Nếu cùng một khách gửi hai request đồng thời:
        // request sau phải chờ request trước hoàn tất,
        // rồi mới đếm lại số đơn và kiểm tra trùng lịch.
        Account customer = accountRepository
                .findByIdForUpdate(customerId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.UNAUTHORIZED,
                        "UNAUTHORIZED",
                        "Tài khoản không còn tồn tại"
                ));

        // JWT filter có thể đã đọc Account trước đó.
        // refresh giúp lấy trạng thái mới nhất thay vì dùng entity đã cache
        // trong persistence context. Khóa vừa lấy vẫn được giữ.
        entityManager.refresh(customer);

        if (customer.isLocked()) {
            throw new BusinessException(
                    HttpStatus.UNAUTHORIZED,
                    "ACCOUNT_LOCKED",
                    "Tài khoản đã bị khóa"
            );
        }

        if (customer.getRole() != AccountRole.CUSTOMER) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN,
                    "FORBIDDEN",
                    "Chức năng này dành cho tài khoản khách hàng"
            );
        }

        var settings = settingsService.load();
        BookingWindow window = validateRequest(request, settings);

        if (reservationRepository.countActiveByCustomer(customerId)
                >= settings.maxActiveReservationsPerCustomer()) {
            throw conflict(
                    "ACTIVE_RESERVATION_LIMIT",
                    "Bạn đã đạt giới hạn "
                            + settings.maxActiveReservationsPerCustomer()
                            + " đơn đang hoạt động"
            );
        }

        if (reservationRepository.existsCustomerOverlap(
                customerId,
                window.start(),
                window.end()
        )) {
            throw conflict(
                    "CUSTOMER_TIME_CONFLICT",
                    "Bạn đã có đơn đặt bàn trùng khoảng thời gian này"
            );
        }

        List<Long> candidateIds = tableRepository.findCandidateIds(
                request.partySize().shortValue(),
                request.requestedAreaId()
        );

        for (Long tableId : candidateIds) {

            // Khóa bàn trước khi kiểm tra và tạo reservation.
            // Hai khách cùng chọn một bàn sẽ lần lượt đi qua đoạn này.
            RestaurantTable table = tableRepository
                    .findByIdForUpdate(tableId)
                    .orElse(null);

            if (table == null) {
                continue;
            }

            entityManager.refresh(table);

            // Bàn có thể đã bị admin chỉnh sửa trong lúc request chờ khóa.
            if (!isSuitable(table, request)) {
                continue;
            }

            // READ_COMMITTED giúp truy vấn này thấy reservation
            // đã được request trước commit sau khi chúng ta chờ khóa.
            if (reservationRepository.existsTableOverlap(
                    tableId,
                    window.start(),
                    window.occupiedEnd()
            )) {
                continue;
            }

            // Khoảng bảo trì/ngưng phục vụ cũng phải được kiểm tra.
            //
            // Luồng tạo TableSuspension cần khóa cùng row RestaurantTable.
            // Constraint chống trùng reservation không tự kiểm tra bảng
            // table_suspension.
            if (!suspensionRepository.findOverlapping(
                    tableId,
                    window.start(),
                    window.occupiedEnd()
            ).isEmpty()) {
                continue;
            }

            // Chờ khóa có thể mất thời gian.
            // Kiểm tra lại giới hạn đặt trước ngay trước khi tạo đơn.
            validateAdvanceTime(
                    window.start(),
                    window.advanceHours()
            );

            Reservation reservation = buildReservation(
                    customer,
                    table,
                    request,
                    settings,
                    window
            );

            reservationRepository.saveAndFlush(reservation);

            saveCreatedLog(reservation, customer);

            ReservationResponse response = toResponse(reservation);

            if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
                // Chỉ phát sự kiện ở đây.
                // Listener bên dưới chỉ gửi email SAU KHI COMMIT thành công.
                eventPublisher.publishEvent(
                        new ReservationConfirmedEvent(
                                customer.getEmail(),
                                response
                        )
                );
            }

            // Spring commit sau khi method hoàn tất.
            // Reservation và status log cùng thành công hoặc cùng rollback.
            return response;
        }

        throw conflict(
                "NO_AVAILABLE_TABLE",
                "Không còn bàn phù hợp trong khoảng thời gian đã chọn"
        );
    }

    private BookingWindow validateRequest(
            CreateReservationRequest request,
            ReservationSettingsService.Settings settings
    ) {
        if (request.partySize() > settings.maxPartySize()) {
            throw invalid(
                    "INVALID_PARTY_SIZE",
                    "Số người vượt quá giới hạn đặt bàn trực tuyến"
            );
        }

        // Dù frontend gửi +07:00 hay UTC, quy tắc giờ mở cửa
        // luôn được xét theo giờ của nhà hàng.
        var localStart = request.startTime()
                .atZoneSameInstant(RESTAURANT_ZONE);

        var localEnd = request.endTime()
                .atZoneSameInstant(RESTAURANT_ZONE);

        Instant start = localStart.toInstant();
        Instant end = localEnd.toInstant();
        LocalDate date = localStart.toLocalDate();

        if (!end.isAfter(start)) {
            throw invalid(
                    "INVALID_TIME_RANGE",
                    "Giờ kết thúc phải sau giờ bắt đầu"
            );
        }

        if (!date.equals(localEnd.toLocalDate())) {
            throw invalid(
                    "INVALID_TIME_RANGE",
                    "Giờ bắt đầu và kết thúc phải trong cùng một ngày"
            );
        }

        if (!isOnTimeGrid(
                localStart.toLocalTime(),
                settings.slotStepMinutes()
        ) || !isOnTimeGrid(
                localEnd.toLocalTime(),
                settings.slotStepMinutes()
        )) {
            throw invalid(
                    "INVALID_TIME_STEP",
                    "Giờ bắt đầu và kết thúc phải theo bước "
                            + settings.slotStepMinutes()
                            + " phút, không có giây"
            );
        }

        long durationMinutes = Duration.between(start, end).toMinutes();

        if (durationMinutes < settings.minDurationMinutes()
                || durationMinutes > settings.maxDurationMinutes()) {
            throw invalid(
                    "INVALID_DURATION",
                    "Thời lượng phải từ "
                            + settings.minDurationMinutes()
                            + " đến "
                            + settings.maxDurationMinutes()
                            + " phút"
            );
        }

        LocalDate today = clock.instant()
                .atZone(RESTAURANT_ZONE)
                .toLocalDate();

        // Giới hạn theo ngày lịch của nhà hàng.
        // Ví dụ maxAdvanceDays = 30: được đặt đến hết ngày hôm nay + 30.
        if (date.isBefore(today)
                || date.isAfter(today.plusDays(settings.maxAdvanceDays()))) {
            throw invalid(
                    "INVALID_RESERVATION_DATE",
                    "Ngày đặt phải từ hôm nay đến "
                            + today.plusDays(settings.maxAdvanceDays())
            );
        }

        int advanceHours =
                request.partySize() > settings.autoConfirmThreshold()
                        ? settings.minAdvanceHoursLargeGroup()
                        : settings.minAdvanceHours();

        validateAdvanceTime(start, advanceHours);

        if (request.requestedAreaId() != null
                && !areaRepository.existsById(request.requestedAreaId())) {
            throw invalid(
                    "INVALID_AREA",
                    "Khu vực được chọn không tồn tại"
            );
        }

        if (closedDayRepository.existsByClosedDate(date)) {
            throw invalid(
                    "RESTAURANT_CLOSED",
                    "Nhà hàng đóng cửa vào ngày đã chọn"
            );
        }

        // Java: thứ Hai = 1 ... Chủ nhật = 7.
        // Database hiện tại: Chủ nhật = 0 ... thứ Bảy = 6.
        short dayOfWeek = (short) (date.getDayOfWeek().getValue() % 7);

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

        Instant opensAt = date
                .atTime(openingHour.getOpenTime())
                .atZone(RESTAURANT_ZONE)
                .toInstant();

        Instant closesAt = date
                .atTime(openingHour.getCloseTime())
                .atZone(RESTAURANT_ZONE)
                .toInstant();

        // Khách phải kết thúc dùng bữa trước hoặc đúng giờ đóng cửa.
        // Buffer dọn bàn được phép kéo dài sau giờ đóng cửa.
        if (start.isBefore(opensAt) || end.isAfter(closesAt)) {
            throw invalid(
                    "OUTSIDE_OPENING_HOURS",
                    "Khoảng thời gian đặt nằm ngoài giờ phục vụ"
            );
        }

        Instant occupiedEnd = end.plus(
                settings.bufferMinutes(),
                ChronoUnit.MINUTES
        );

        return new BookingWindow(
                start,
                end,
                occupiedEnd,
                date,
                advanceHours
        );
    }

    private void validateAdvanceTime(Instant start, int advanceHours) {
        Instant earliestStart = clock.instant()
                .plus(advanceHours, ChronoUnit.HOURS);

        if (start.isBefore(earliestStart)) {
            throw invalid(
                    "INSUFFICIENT_ADVANCE_TIME",
                    "Vui lòng đặt trước ít nhất "
                            + advanceHours
                            + " giờ"
            );
        }
    }

    private boolean isOnTimeGrid(LocalTime time, int stepMinutes) {
        return time.getSecond() == 0
                && time.getNano() == 0
                && time.getMinute() % stepMinutes == 0;
    }

    private boolean isSuitable(
            RestaurantTable table,
            CreateReservationRequest request
    ) {
        int partySize = request.partySize();

        return table.isActive()
                && table.getMinCapacity() <= partySize
                && table.getMaxCapacity() >= partySize
                && (
                request.requestedAreaId() == null
                        || request.requestedAreaId()
                        .equals(table.getArea().getId())
        );
    }

    private Reservation buildReservation(
            Account customer,
            RestaurantTable table,
            CreateReservationRequest request,
            ReservationSettingsService.Settings settings,
            BookingWindow window
    ) {
        Reservation reservation = new Reservation();

        reservation.setCode(generateCode(window.date()));
        reservation.setCustomer(customer);
        reservation.setTable(table);

        // Đây là lựa chọn ban đầu của khách.
        // Khách không chọn khu vực thì lưu null, dù bàn được gán có khu vực.
        reservation.setRequestedArea(
                request.requestedAreaId() == null
                        ? null
                        : table.getArea()
        );

        reservation.setPartySize(request.partySize().shortValue());
        reservation.setStartTime(window.start());
        reservation.setEndTime(window.end());

        // Lưu snapshot buffer trên đơn.
        // Admin đổi buffer sau này sẽ không làm thay đổi đơn đã tạo.
        reservation.setBufferMinutes((short) settings.bufferMinutes());

        reservation.setStatus(
                request.partySize() <= settings.autoConfirmThreshold()
                        ? ReservationStatus.CONFIRMED
                        : ReservationStatus.PENDING
        );

        reservation.setSpecialNote(normalizeNote(request.specialNote()));
        reservation.setRescheduleCount((short) 0);

        // created_by dùng cho nhân viên đặt hộ khách.
        // Khách tự đặt thì customer_id có giá trị, created_by để null.
        reservation.setCreatedBy(null);

        return reservation;
    }

    private String generateCode(LocalDate date) {
        String datePart = date.format(
                DateTimeFormatter.ofPattern("yyMMdd")
        );

        String randomPart = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 8)
                .toUpperCase(Locale.ROOT);

        // Tổng cộng 20 ký tự, khớp VARCHAR(20).
        // Unique constraint trong DB vẫn là bảo đảm cuối cùng.
        return "SAGE-" + datePart + "-" + randomPart;
    }

    private String normalizeNote(String note) {
        return note == null || note.isBlank() ? null : note.trim();
    }

    private void saveCreatedLog(
            Reservation reservation,
            Account customer
    ) {
        // Dùng LinkedHashMap vì requestedAreaId và specialNote có thể null.
        // Map.of(...) không cho phép giá trị null.
        Map<String, Object> newValue = new LinkedHashMap<>();

        newValue.put("code", reservation.getCode());
        newValue.put("status", reservation.getStatus().name());
        newValue.put("tableId", reservation.getTable().getId());
        newValue.put("partySize", reservation.getPartySize());
        newValue.put("startTime", reservation.getStartTime().toString());
        newValue.put("endTime", reservation.getEndTime().toString());
        newValue.put("bufferMinutes", reservation.getBufferMinutes());
        newValue.put("specialNote", reservation.getSpecialNote());
        newValue.put(
                "requestedAreaId",
                reservation.getRequestedArea() == null
                        ? null
                        : reservation.getRequestedArea().getId()
        );

        ReservationStatusLog statusLog = new ReservationStatusLog();

        statusLog.setReservation(reservation);
        statusLog.setEventType(ReservationLogEventType.CREATED);
        statusLog.setOldValue(null);
        statusLog.setNewValue(newValue);
        statusLog.setChangedBy(customer);

        // Nếu lưu log thất bại, reservation vừa tạo cũng bị rollback.
        statusLogRepository.saveAndFlush(statusLog);
    }

    private ReservationResponse toResponse(Reservation reservation) {
        Area area = reservation.getRequestedArea();

        ReservationResponse.AreaSummary requestedArea = area == null
                ? null
                : new ReservationResponse.AreaSummary(
                area.getId(),
                area.getName(),
                area.getDescription()
        );

        // Map DTO ngay trong transaction để đọc các quan hệ LAZY an toàn.
        return new ReservationResponse(
                reservation.getId(),
                reservation.getCode(),
                reservation.getPartySize(),
                toRestaurantTime(reservation.getStartTime()),
                toRestaurantTime(reservation.getEndTime()),
                reservation.getStatus(),
                requestedArea,
                reservation.getSpecialNote(),
                reservation.getRescheduleCount(),
                reservation.getRejectionReason(),
                reservation.getCancellationReason(),
                toRestaurantTime(reservation.getCreatedAt())
        );
    }

    private OffsetDateTime toRestaurantTime(Instant instant) {
        return instant.atZone(RESTAURANT_ZONE).toOffsetDateTime();
    }

    private BusinessException invalid(String code, String message) {
        return new BusinessException(HttpStatus.BAD_REQUEST, code, message);
    }

    private BusinessException conflict(String code, String message) {
        return new BusinessException(HttpStatus.CONFLICT, code, message);
    }
}