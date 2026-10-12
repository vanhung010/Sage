package com.sagares.saga_res_api.restaurant.service;

import com.sagares.saga_res_api.common.exception.BusinessException;
import com.sagares.saga_res_api.restaurant.dto.OpeningHourResponse;
import com.sagares.saga_res_api.restaurant.dto.admin.AdminOpeningHourResponse;
import com.sagares.saga_res_api.restaurant.dto.admin.CreateOpeningHourRequest;
import com.sagares.saga_res_api.restaurant.dto.admin.UpdateOpeningHourRequest;
import com.sagares.saga_res_api.restaurant.entity.OpeningHour;
import com.sagares.saga_res_api.restaurant.repository.OpeningHourRepository;
import com.sagares.saga_res_api.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OpeningHourService {

    private final OpeningHourRepository openingHourRepository;
    private final ReservationRepository reservationRepository;

    public List<OpeningHourResponse> getPublicOpeningHours() {
        return sortedOpeningHours().stream()
                .map(hour -> new OpeningHourResponse(toDayOfWeek(hour.getDayOfWeek()),
                        hour.getOpenTime(), hour.getCloseTime(), hour.isClosed()))
                .toList();
    }

    public List<AdminOpeningHourResponse> listAdminOpeningHours() {
        return sortedOpeningHours().stream().map(this::toAdminResponse).toList();
    }

    @Transactional
    public AdminOpeningHourResponse createOpeningHour(CreateOpeningHourRequest request) {
        if (request.dayOfWeek() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_DAY_OF_WEEK",
                    "Ngày trong tuần là bắt buộc");
        }
        validateTimes(request.openTime(), request.closeTime());
        short id = (short) (request.dayOfWeek().getValue() % 7);
        if (openingHourRepository.insertIfAbsent(id, request.openTime(), request.closeTime()) == 0) {
            throw new BusinessException(HttpStatus.CONFLICT, "OPENING_HOUR_ALREADY_EXISTS",
                    "Giờ mở cửa của ngày này đã được cấu hình");
        }
        return new AdminOpeningHourResponse(id, request.dayOfWeek(),
                request.openTime(), request.closeTime(), false);
    }

    @Transactional
    public AdminOpeningHourResponse updateOpeningHour(Short id, UpdateOpeningHourRequest request) {
        OpeningHour hour = openingHourRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "OPENING_HOUR_NOT_FOUND",
                        "Cấu hình giờ mở cửa không tồn tại"));
        if (request.dayOfWeek() != null && request.dayOfWeek() != toDayOfWeek(hour.getDayOfWeek())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "OPENING_HOUR_DAY_IMMUTABLE",
                    "Không thể thay đổi ngày trong tuần của cấu hình giờ mở cửa");
        }
        if (request.dayOfWeek() == null && request.openTime() == null && request.closeTime() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "EMPTY_PATCH",
                    "Ít nhất một trường giờ mở cửa phải được cung cấp");
        }
        LocalTime openTime = request.openTime() == null ? hour.getOpenTime() : request.openTime();
        LocalTime closeTime = request.closeTime() == null ? hour.getCloseTime() : request.closeTime();
        validateTimes(openTime, closeTime);
        hour.setOpenTime(openTime);
        hour.setCloseTime(closeTime);
        openingHourRepository.saveAndFlush(hour);
        List<Long> impactedReservationIds = reservationRepository.findIdsOutsideOpeningHours(
                id, openTime, closeTime, hour.isClosed());
        return new AdminOpeningHourResponse(id, toDayOfWeek(id), openTime, closeTime,
                hour.isClosed(), impactedReservationIds);
    }

    private List<OpeningHour> sortedOpeningHours() {
        return openingHourRepository.findAll().stream()
                .sorted(Comparator.comparingInt(hour -> toDayOfWeek(hour.getDayOfWeek()).getValue()))
                .toList();
    }

    private DayOfWeek toDayOfWeek(short dayOfWeek) {
        return DayOfWeek.of(dayOfWeek == 0 ? 7 : dayOfWeek);
    }

    private void validateTimes(LocalTime openTime, LocalTime closeTime) {
        if (openTime == null || closeTime == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "OPENING_HOUR_TIME_REQUIRED",
                    "Giờ mở cửa và giờ đóng cửa là bắt buộc");
        }
        if (!openTime.isBefore(closeTime)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_OPENING_HOUR_TIME",
                    "Giờ đóng cửa phải sau giờ mở cửa");
        }
    }

    private AdminOpeningHourResponse toAdminResponse(OpeningHour hour) {
        return new AdminOpeningHourResponse(hour.getDayOfWeek(), toDayOfWeek(hour.getDayOfWeek()),
                hour.getOpenTime(), hour.getCloseTime(), hour.isClosed());
    }
}
