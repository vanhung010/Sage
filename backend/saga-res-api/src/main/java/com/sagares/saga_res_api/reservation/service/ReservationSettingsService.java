package com.sagares.saga_res_api.reservation.service;

import com.sagares.saga_res_api.common.exception.BusinessException;
import com.sagares.saga_res_api.restaurant.entity.BusinessConfig;
import com.sagares.saga_res_api.restaurant.repository.BusinessConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationSettingsService {

    public static final ZoneId RESTAURANT_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final BusinessConfigRepository businessConfigRepository;

    public record Settings(
            //Thời gian dọn bàn sau khi khách kết thúc lượt ăn. Trong thời gian này, bàn chưa nhận lượt mới.
            int bufferMinutes,
            //Thời gian tối thiểu phải đặt trước đối với nhóm được tự động xác nhận.
            int minAdvanceHours,
            //Thời gian tối thiểu phải đặt trước đối với nhóm đông cần nhân viên duyệt.
            int minAdvanceHoursLargeGroup,
            //Giới hạn ngày đặt trong tương lai. Code hiện tại cho phép chọn đến ngày hôm nay cộng 30 ngày.
            int maxAdvanceDays,
            //Số người tối đa trong một đơn đặt bàn trực tuyến.
            int maxPartySize,
            //Nhóm có số người không vượt quá ngưỡng này được tự xác nhận nếu đặt thành công; vượt ngưỡng thì chờ duyệt.
            int autoConfirmThreshold,
            //Khoảng cách giữa các giờ bắt đầu được xét: 10:00, 10:15, 10:30…
            int slotStepMinutes,
            //Thời lượng dùng bàn ngắn nhất khách được chọn.
            int minDurationMinutes,
            //Thời lượng dùng bàn dài nhất khách được chọn.
            int maxDurationMinutes
    ) {
    }

    public Settings load() {
        Map<String, String> values = businessConfigRepository
                .findAll()
                .stream()
                .collect(Collectors.toMap(BusinessConfig::getConfigKey, BusinessConfig::getConfigValue));

        Settings settings = new Settings(
                readInt(values, "buffer_minutes"),
                readInt(values, "min_advance_hours"),
                readInt(values, "min_advance_hours_large_group"),
                readInt(values, "max_advance_days"),
                readInt(values, "max_party_size"),
                readInt(values, "auto_confirm_threshold"),
                readInt(values, "time_slot_step_minutes"),
                readInt(values, "min_duration_minutes"),
                readInt(values, "max_duration_minutes")
        );

        validate(settings);

        int largeGroupThreshold = readInt(values, "large_group_threshold");

        if (largeGroupThreshold != settings.autoConfirmThreshold() + 1) {
            throw invalidConfig(
                    "large_group_threshold phải bằng "
                            + "auto_confirm_threshold + 1"
            );
        }

        return settings;
    }

    private int readInt(
            Map<String, String> values,
            String key
    ) {
        String value = values.get(key);

        if (value == null || value.isBlank()) {
            throw invalidConfig("Thiếu cấu hình: " + key);
        }

        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            throw invalidConfig(
                    "Cấu hình phải là số nguyên: " + key
            );
        }
    }

    private void validate(Settings settings) {
        if (settings.bufferMinutes() < 0
                || settings.minAdvanceHours() < 0
                || settings.minAdvanceHoursLargeGroup()
                < settings.minAdvanceHours()
                || settings.maxAdvanceDays() < 0) {
            throw invalidConfig(
                    "Cấu hình thời gian đặt bàn không hợp lệ"
            );
        }

        // 12 là giới hạn cứng của schema hiện tại.
        if (settings.maxPartySize() < 1
                || settings.maxPartySize() > 12
                || settings.autoConfirmThreshold() < 1
                || settings.autoConfirmThreshold() > 12) {
            throw invalidConfig(
                    "Cấu hình số người không hợp lệ"
            );
        }

        // Giữ contract hiện tại: các mốc thời gian cách nhau 15 phút.
        if (settings.slotStepMinutes() != 15) {
            throw invalidConfig(
                    "time_slot_step_minutes phải bằng 15 "
                            + "theo API contract hiện tại"
            );
        }

        if (settings.minDurationMinutes() < 60
                || settings.maxDurationMinutes() > 180
                || settings.minDurationMinutes()
                > settings.maxDurationMinutes()
                || settings.minDurationMinutes()
                % settings.slotStepMinutes() != 0
                || settings.maxDurationMinutes()
                % settings.slotStepMinutes() != 0) {
            throw invalidConfig(
                    "Thời lượng phải nằm trong 60–180 phút "
                            + "và chia hết cho bước thời gian"
            );
        }
    }

    private BusinessException invalidConfig(String message) {
        return new BusinessException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "RESERVATION_CONFIG_INVALID",
                message
        );
    }
}
