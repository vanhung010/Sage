package com.sagares.saga_res_api.reservation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public record CreateGuestReservationRequest(

        @NotBlank(message = "Vui lòng nhập tên khách")
        @Size(max = 255, message = "Tên khách tối đa 255 ký tự")
        String name,

        @NotBlank(message = "Vui lòng nhập số điện thoại")
        @Pattern(
                regexp = "[0-9]{10}",
                message = "Số điện thoại phải gồm 10 chữ số"
        )
        String phone,

        @NotNull(message = "Vui lòng chọn ngày đặt bàn")
        LocalDate date,

        @NotNull(message = "Vui lòng chọn giờ bắt đầu")
        LocalTime timeStart,

        @NotNull(message = "Vui lòng chọn giờ kết thúc")
        LocalTime timeEnd,

        @NotNull(message = "Vui lòng nhập số khách")
        @Min(value = 1, message = "Số khách phải lớn hơn 0")
        @Max(value = 12, message = "Mỗi đơn tối đa 12 khách")
        Integer partySize,

        @Positive(message = "Mã khu vực phải lớn hơn 0")
        Long areaId,

        String description
) {

    public CreateGuestReservationRequest {
        name = name == null ? null : name.trim();
        phone = phone == null ? null : phone.trim();

        description = description == null || description.isBlank()
                ? null
                : description.trim();
    }
}