package com.sagares.saga_res_api.reservation.service;

import com.sagares.saga_res_api.reservation.dto.AvailabilitySlotResponse;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.sagares.saga_res_api.reservation.service
        .ReservationSettingsService.RESTAURANT_ZONE;

@Component
public class AvailabilityCalculator {

    public record BusyInterval(Instant start, Instant end) {}

    public List<AvailabilitySlotResponse> calculate(
            LocalDate date,
            LocalTime openTime,
            LocalTime closeTime,
            Instant earliestStart,
            int durationMinutes,
            int bufferMinutes,
            int stepMinutes,
            //ID của bàn phù hợp.
            //Value: danh sách các khoảng thời gian bàn đó bị chiếm.
            Map<Long, List<BusyInterval>> busyByTable
    ) {
        //Nếu không có bàn return empty
        if (busyByTable.isEmpty()) {
            return List.of();
        }

        ZonedDateTime opensAt = date.atTime(openTime).atZone(RESTAURANT_ZONE);

        ZonedDateTime closesAt = date.atTime(closeTime).atZone(RESTAURANT_ZONE);

        List<AvailabilitySlotResponse> slots = new ArrayList<>();

        // Bắt đầu từ đầu ngày để các mốc luôn đúng lưới
        // 00, 15, 30, 45 phút, kể cả giờ mở cửa là 10:10.
        for (
                ZonedDateTime candidate = date.atStartOfDay(RESTAURANT_ZONE);
                !candidate.plusMinutes(durationMinutes).isAfter(closesAt);
                candidate = candidate.plusMinutes(stepMinutes)
        ) {
            //Nếu giờ đang xét trước giờ mở cửa thì bỏ qua
            if (candidate.isBefore(opensAt)) {
                continue;
            }

            Instant start = candidate.toInstant();

            //nếu giờ bắt đầu đang xét chưa đáp ứng thời gian đặt trước tối thiểu thì bỏ qua giờ đó.
            if (start.isBefore(earliestStart)) {
                continue;
            }

            ZonedDateTime diningEnd = candidate.plusMinutes(durationMinutes);
            //thời gian chiếm
            Instant occupiedEnd = diningEnd.plusMinutes(bufferMinutes).toInstant();

            boolean hasFreeTable = false;

            for (List<BusyInterval> intervals : busyByTable.values()) {
                boolean tableIsFree = true;

                // Kiểm tra tất cả khoảng bận của bàn hiện tại.
                for (BusyInterval busy : intervals) {
                    boolean conflict = overlaps(
                            start,
                            occupiedEnd,
                            busy.start(),
                            busy.end()
                    );

                    if (conflict) {
                        tableIsFree = false;
                        break; // Bàn này bận, không cần kiểm tra tiếp.
                    }
                }

                if (tableIsFree) {
                    hasFreeTable = true;
                    break; // Đã tìm được một bàn trống.
                }
            }

            if (hasFreeTable) {
                slots.add(new AvailabilitySlotResponse(
                        candidate.toOffsetDateTime(),
                        diningEnd.toOffsetDateTime()
                ));
            }
        }

        return List.copyOf(slots);
    }

    private boolean overlaps(
            Instant firstStart,
            Instant firstEnd,
            Instant secondStart,
            Instant secondEnd
    ) {
        return firstStart.isBefore(secondEnd)
                && secondStart.isBefore(firstEnd);
    }
}