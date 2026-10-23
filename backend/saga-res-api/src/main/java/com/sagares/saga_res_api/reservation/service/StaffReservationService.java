package com.sagares.saga_res_api.reservation.service;

import com.sagares.saga_res_api.account.entity.Account;
import com.sagares.saga_res_api.common.dto.PageResponse;
import com.sagares.saga_res_api.common.exception.BusinessException;
import com.sagares.saga_res_api.reservation.dto.StaffReservationStatisticsResponse;
import com.sagares.saga_res_api.reservation.dto.StaffReservationSummaryResponse;
import com.sagares.saga_res_api.reservation.entity.Reservation;
import com.sagares.saga_res_api.reservation.entity.ReservationStatus;
import com.sagares.saga_res_api.reservation.repository.ReservationRepository;
import com.sagares.saga_res_api.reservation.specification.ReservationSpecifications;
import com.sagares.saga_res_api.table.entity.Area;
import com.sagares.saga_res_api.table.entity.RestaurantTable;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class StaffReservationService {

    private static final ZoneId RESTAURANT_ZONE =
            ZoneId.of("Asia/Ho_Chi_Minh");

    private final ReservationRepository reservationRepository;

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