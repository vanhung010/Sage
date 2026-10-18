package com.sagares.saga_res_api.reservation.service;

import com.sagares.saga_res_api.common.exception.BusinessException;
import com.sagares.saga_res_api.reservation.dto.AvailabilityResponse;
import com.sagares.saga_res_api.reservation.entity.ReservationStatus;
import com.sagares.saga_res_api.reservation.dto.AvailabilitySlotResponse;
import com.sagares.saga_res_api.reservation.entity.Reservation;
import com.sagares.saga_res_api.reservation.repository.ReservationRepository;
import com.sagares.saga_res_api.reservation.service.AvailabilityCalculator.BusyInterval;
import com.sagares.saga_res_api.reservation.service.ReservationSettingsService.Settings;
import com.sagares.saga_res_api.restaurant.entity.OpeningHour;
import com.sagares.saga_res_api.restaurant.repository.OpeningHourRepository;
import com.sagares.saga_res_api.restaurant.repository.SpecialClosedDayRepository;
import com.sagares.saga_res_api.table.entity.RestaurantTable;
import com.sagares.saga_res_api.table.entity.TableSuspension;
import com.sagares.saga_res_api.table.repository.AreaRepository;
import com.sagares.saga_res_api.table.repository.RestaurantTableRepository;
import com.sagares.saga_res_api.table.repository.TableSuspensionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.sagares.saga_res_api.reservation.service
        .ReservationSettingsService.RESTAURANT_ZONE;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AvailabilityService {

    private final ReservationSettingsService settingsService;
    private final AvailabilityCalculator calculator;

    private final OpeningHourRepository openingHourRepository;
    private final SpecialClosedDayRepository specialClosedDayRepository;

    private final AreaRepository areaRepository;
    private final RestaurantTableRepository tableRepository;
    private final ReservationRepository reservationRepository;
    private final TableSuspensionRepository suspensionRepository;

    private final Clock clock;

    // API availability công khai hiện tại tiếp tục gọi hàm này.
    public AvailabilityResponse search(
            LocalDate date,
            int partySize,
            int durationMinutes,
            Long areaId
    ) {
        return search(
                date,
                partySize,
                durationMinutes,
                areaId,
                null,
                null,
                clock.instant()
        );
    }

    // Chỉ gọi sau khi đã kiểm tra chủ đơn và điều kiện đổi lịch.
// Package-private: dùng trong cùng package service.
    AvailabilityResponse searchForReschedule(
            LocalDate date,
            int partySize,
            int durationMinutes,
            Long areaId,
            Reservation reservation,
            Instant now
    ) {
        return search(
                date,
                partySize,
                durationMinutes,
                areaId,
                reservation.getId(),
                reservation.getCustomer().getId(),
                now
        );
    }

    // Phần tính giờ trống dùng chung.
    private AvailabilityResponse search(
            LocalDate date,
            int partySize,
            int durationMinutes,
            Long areaId,
            Long excludedReservationId,
            Long customerId,
            Instant now
    ) {
        Settings settings = settingsService.load();

        // Kiểm tra ngày, số khách, thời lượng và khu vực mới.
        validateRequest(
                date,
                partySize,
                durationMinutes,
                areaId,
                settings,
                now
        );

        OpeningHour openingHour = loadOpeningHour(date);

        List<RestaurantTable> tables = findSuitableTables(
                partySize,
                areaId
        );

        if (tables.isEmpty()) {
            return response(
                    date,
                    partySize,
                    durationMinutes,
                    areaId,
                    List.of()
            );
        }

        Instant rangeStart = date
                .atTime(openingHour.getOpenTime())
                .atZone(RESTAURANT_ZONE)
                .toInstant();

        // Bao phủ cả buffer của slot cuối cùng.
        Instant rangeEnd = date
                .atTime(openingHour.getCloseTime())
                .atZone(RESTAURANT_ZONE)
                .plusMinutes(settings.bufferMinutes())
                .toInstant();

        // Đọc dữ liệu chiếm bàn một lần cho cả ngày.
        List<Reservation> reservations = reservationRepository
                .findTableOccupancyInRange(
                        rangeStart,
                        rangeEnd
                );

        Map<Long, List<BusyInterval>> busyByTable =
                loadBusyIntervals(
                        tables,
                        rangeStart,
                        rangeEnd,
                        reservations,
                        excludedReservationId
                );

        // Thời gian đặt trước được tính theo số khách mới.
        int advanceHours =
                partySize > settings.autoConfirmThreshold()
                        ? settings.minAdvanceHoursLargeGroup()
                        : settings.minAdvanceHours();

        Instant earliestStart = now.plus(
                advanceHours,
                ChronoUnit.HOURS
        );

        List<AvailabilitySlotResponse> slots = calculator.calculate(
                date,
                openingHour.getOpenTime(),
                openingHour.getCloseTime(),
                earliestStart,
                durationMinutes,
                settings.bufferMinutes(),
                settings.slotStepMinutes(),
                busyByTable
        );

        // Khi đổi lịch, loại thêm các giờ trùng với đơn khác của khách.
        if (customerId != null) {
            List<BusyInterval> customerIntervals = reservations
                    .stream()

                    // Không coi chính đơn đang sửa là lịch trùng.
                    .filter(reservation ->
                            !reservation.getId()
                                    .equals(excludedReservationId)
                    )

                    // Xét cả đơn của khách ở bàn/khu vực khác.
                    .filter(reservation ->
                            reservation.getCustomer() != null
                                    && customerId.equals(
                                    reservation.getCustomer().getId()
                            )
                    )

                    .filter(reservation ->
                            reservation.getStatus()
                                    == ReservationStatus.PENDING
                                    || reservation.getStatus()
                                    == ReservationStatus.CONFIRMED
                                    || reservation.getStatus()
                                    == ReservationStatus.ARRIVED
                    )

                    // Lịch khách không cộng buffer dọn bàn.
                    .map(reservation -> new BusyInterval(
                            reservation.getStartTime(),
                            reservation.getEndTime()
                    ))
                    .toList();

            slots = slots.stream()
                    .filter(slot ->
                            customerIntervals.stream()
                                    .noneMatch(busy ->
                                            slot.startTime()
                                                    .toInstant()
                                                    .isBefore(busy.end())
                                                    && busy.start().isBefore(
                                                    slot.endTime().toInstant()
                                            )
                                    )
                    )
                    .toList();
        }

        return response(
                date,
                partySize,
                durationMinutes,
                areaId,
                slots
        );
    }

    private void validateRequest(
            LocalDate date,
            int partySize,
            int durationMinutes,
            Long areaId,
            Settings settings,
            Instant now
    ) {
        if (date == null) {
            throw badRequest(
                    "DATE_REQUIRED",
                    "Ngày đặt bàn là bắt buộc"
            );
        }

        LocalDate today = now
                .atZone(RESTAURANT_ZONE)
                .toLocalDate();

        LocalDate lastAllowedDate =
                today.plusDays(settings.maxAdvanceDays());

        if (date.isBefore(today)
                || date.isAfter(lastAllowedDate)) {
            throw badRequest(
                    "INVALID_RESERVATION_DATE",
                    "Ngày đặt bàn phải từ "
                            + today
                            + " đến "
                            + lastAllowedDate
            );
        }

        if (partySize < 1
                || partySize > settings.maxPartySize()) {
            throw badRequest(
                    "INVALID_PARTY_SIZE",
                    "Số người phải từ 1 đến "
                            + settings.maxPartySize()
            );
        }

        if (durationMinutes < settings.minDurationMinutes()
                || durationMinutes > settings.maxDurationMinutes()
                || durationMinutes
                % settings.slotStepMinutes() != 0) {
            throw badRequest(
                    "INVALID_DURATION",
                    "Thời lượng phải từ "
                            + settings.minDurationMinutes()
                            + " đến "
                            + settings.maxDurationMinutes()
                            + " phút và chia hết cho "
                            + settings.slotStepMinutes()
            );
        }

        if (areaId != null) {
            if (areaId <= 0 || !areaRepository.existsById(areaId)) {
                throw badRequest(
                        "INVALID_AREA",
                        "Khu vực được chọn không tồn tại"
                );
            }
        }
    }

    private OpeningHour loadOpeningHour(LocalDate date) {
        if (specialClosedDayRepository.existsByClosedDate(date)) {
            throw badRequest(
                    "RESTAURANT_CLOSED",
                    "Nhà hàng đóng cửa vào ngày đã chọn"
            );
        }

        // DB: Chủ nhật = 0, thứ hai = 1, ..., thứ bảy = 6.
        short dayOfWeek = (short) (date.getDayOfWeek().getValue() % 7);

        OpeningHour openingHour = openingHourRepository
                .findById(dayOfWeek)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "OPENING_HOUR_NOT_CONFIGURED",
                        "Chưa cấu hình giờ mở cửa cho ngày này"
                ));

        if (openingHour.isClosed()) {
            throw badRequest(
                    "RESTAURANT_CLOSED",
                    "Nhà hàng đóng cửa vào ngày đã chọn"
            );
        }

        if (openingHour.getOpenTime() == null
                || openingHour.getCloseTime() == null
                || !openingHour.getOpenTime()
                .isBefore(openingHour.getCloseTime())) {
            throw new BusinessException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "INVALID_OPENING_HOUR_CONFIG",
                    "Cấu hình giờ mở cửa không hợp lệ"
            );
        }

        return openingHour;
    }

    private List<RestaurantTable> findSuitableTables(int partySize, Long areaId) {
        List<RestaurantTable> tables = (areaId == null) ? tableRepository.findByActiveTrueOrderByMaxCapacityAscCodeAsc() : tableRepository.findByAreaIdAndActiveTrueOrderByMaxCapacityAscCodeAsc(areaId);

        return tables.stream().filter(table ->
                        table.getMinCapacity() <= partySize && partySize <= table.getMaxCapacity())
                .toList();
    }

    private Map<Long, List<BusyInterval>> loadBusyIntervals(
            List<RestaurantTable> tables,
            Instant rangeStart,
            Instant rangeEnd,
            List<Reservation> reservations,
            Long excludedReservationId
    ) {
        Map<Long, List<BusyInterval>> busyByTable = new HashMap<>();

        // Bàn chưa có reservation/suspension vẫn phải được xét.
        for (RestaurantTable table : tables) {
            busyByTable.put(
                    table.getId(),
                    new ArrayList<>()
            );
        }

        for (Reservation reservation : reservations) {
            // Chỉ bỏ qua chính đơn đang sửa.
            // Các đơn khác trên cùng bàn vẫn phải chặn giờ.
            if (reservation.getId().equals(excludedReservationId)) {
                continue;
            }

            Long tableId = reservation.getTable().getId();

            List<BusyInterval> intervals = busyByTable.get(tableId);

            // Bàn này không thuộc danh sách bàn phù hợp.
            if (intervals == null) {
                continue;
            }

            // Dùng buffer snapshot đã lưu trên từng đơn.
            Instant occupiedEnd = reservation.getEndTime()
                    .plus(
                            reservation.getBufferMinutes(),
                            ChronoUnit.MINUTES
                    );

            intervals.add(new BusyInterval(
                    reservation.getStartTime(),
                    occupiedEnd
            ));
        }

        // Suspension của bàn vẫn luôn được xét.
        List<TableSuspension> suspensions = suspensionRepository
                .findAllOverlapping(
                        rangeStart,
                        rangeEnd
                );

        for (TableSuspension suspension : suspensions) {
            Long tableId = suspension.getTable().getId();

            List<BusyInterval> intervals = busyByTable.get(tableId);

            if (intervals == null) {
                continue;
            }

            intervals.add(new BusyInterval(
                    suspension.getStartTime(),
                    suspension.getEndTime()
            ));
        }

        return busyByTable;
    }

    private AvailabilityResponse response(
            LocalDate date,
            int partySize,
            int durationMinutes,
            Long areaId,
            List<AvailabilitySlotResponse> slots
    ) {
        return new AvailabilityResponse(
                date,
                partySize,
                durationMinutes,
                areaId,
                slots,
                List.of()
        );
    }

    private BusinessException badRequest(
            String code,
            String message
    ) {
        return new BusinessException(
                HttpStatus.BAD_REQUEST,
                code,
                message
        );
    }
}