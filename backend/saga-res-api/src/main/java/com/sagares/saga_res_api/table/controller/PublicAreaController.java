package com.sagares.saga_res_api.table.controller;

import com.sagares.saga_res_api.table.dto.publics.PublicAreaResponse;
import com.sagares.saga_res_api.table.service.PublicAreaService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/areas")
@RequiredArgsConstructor
public class PublicAreaController {

    private final PublicAreaService publicAreaService;

    @GetMapping
    @Operation(
            summary = "Lấy danh sách khu vực để chọn khi đặt bàn",
            description = """
                    Chỉ trả khu vực đang hoạt động, sắp xếp theo tên.
                    Không bắt buộc đăng nhập.
                    """
    )
    public List<PublicAreaResponse> getAreas() {
        return publicAreaService.getAreas();
    }
}
