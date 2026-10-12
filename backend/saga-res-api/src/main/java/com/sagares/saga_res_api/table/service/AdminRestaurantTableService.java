package com.sagares.saga_res_api.table.service;

import com.sagares.saga_res_api.common.exception.BusinessException;
import com.sagares.saga_res_api.table.dto.admin.AdminRestaurantTableResponse;
import com.sagares.saga_res_api.table.dto.admin.AdminTableAreaResponse;
import com.sagares.saga_res_api.table.dto.admin.CreateRestaurantTableRequest;
import com.sagares.saga_res_api.table.dto.admin.UpdateRestaurantTableRequest;
import com.sagares.saga_res_api.table.entity.Area;
import com.sagares.saga_res_api.table.entity.RestaurantTable;
import com.sagares.saga_res_api.table.repository.AreaRepository;
import com.sagares.saga_res_api.table.repository.RestaurantTableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminRestaurantTableService {

    private static final int TABLE_CODE_MAX_LENGTH = 20;

    private final RestaurantTableRepository restaurantTableRepository;
    private final AreaRepository areaRepository;

    public List<AdminRestaurantTableResponse> listTables() {
        return restaurantTableRepository.findAllByOrderByArea_NameAscCodeAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AdminRestaurantTableResponse createTable(CreateRestaurantTableRequest request) {
        String code = normalizeCode(request.code());
        if (restaurantTableRepository.existsByCode(code)) {
            throw duplicateTableCode();
        }
        Area area = findActiveArea(request.areaId());
        validateCapacity(request.minCapacity(), request.maxCapacity());

        RestaurantTable table = new RestaurantTable();
        table.setCode(code);
        table.setArea(area);
        table.setMinCapacity(request.minCapacity());
        table.setMaxCapacity(request.maxCapacity());
        table.setActive(true);
        return toResponse(save(table));
    }

    @Transactional
    public AdminRestaurantTableResponse updateTable(Long id, UpdateRestaurantTableRequest request) {
        if (request.code() == null && request.areaId() == null
                && request.minCapacity() == null && request.maxCapacity() == null) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "EMPTY_PATCH",
                    "Ít nhất một trường của bàn phải được cung cấp."
            );
        }

        RestaurantTable table = findTableForUpdate(id);
        String code = request.code() == null ? table.getCode() : normalizeCode(request.code());
        if (!code.equals(table.getCode()) && restaurantTableRepository.existsByCodeAndIdNot(code, id)) {
            throw duplicateTableCode();
        }
        Area area = table.getArea();
        if (request.areaId() != null && !request.areaId().equals(area.getId())) {
            area = findActiveArea(request.areaId());
        }
        Short minCapacity = request.minCapacity() == null ? table.getMinCapacity() : request.minCapacity();
        Short maxCapacity = request.maxCapacity() == null ? table.getMaxCapacity() : request.maxCapacity();
        validateCapacity(minCapacity, maxCapacity);

        table.setCode(code);
        table.setArea(area);
        table.setMinCapacity(minCapacity);
        table.setMaxCapacity(maxCapacity);
        return toResponse(save(table));
    }

    @Transactional
    public AdminRestaurantTableResponse deactivateTable(Long id) {
        RestaurantTable table = findTableForUpdate(id);
        table.setActive(false);
        return toResponse(save(table));
    }

    private RestaurantTable findTableForUpdate(Long id) {
        return restaurantTableRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "TABLE_NOT_FOUND",
                        "Bàn không tồn tại."
                ));
    }

    private Area findActiveArea(Long id) {
        if (id == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_AREA_ID", "Khu vực là bắt buộc.");
        }
        Area area = areaRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "AREA_NOT_FOUND",
                        "Khu vực không tồn tại."
                ));
        if (!area.isActive()) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "AREA_INACTIVE",
                    "Không thể tạo hoặc chuyển bàn vào khu vực đã ngừng hoạt động."
            );
        }
        return area;
    }

    private String normalizeCode(String code) {
        String normalized = code == null ? "" : code.trim();
        if (normalized.isBlank() || normalized.length() > TABLE_CODE_MAX_LENGTH) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_TABLE_CODE",
                    "Mã bàn không được để trống và tối đa 20 ký tự."
            );
        }
        return normalized;
    }

    private void validateCapacity(Short minCapacity, Short maxCapacity) {
        if (minCapacity == null || minCapacity <= 0) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_TABLE_CAPACITY",
                    "Sức chứa tối thiểu phải lớn hơn 0."
            );
        }
        if (maxCapacity == null || maxCapacity < minCapacity) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_TABLE_CAPACITY",
                    "Sức chứa tối đa không được nhỏ hơn sức chứa tối thiểu."
            );
        }
    }

    private RestaurantTable save(RestaurantTable table) {
        try {
            return restaurantTableRepository.saveAndFlush(table);
        } catch (DataIntegrityViolationException exception) {
            throw duplicateTableCode();
        }
    }

    private BusinessException duplicateTableCode() {
        return new BusinessException(HttpStatus.CONFLICT, "TABLE_CODE_ALREADY_EXISTS", "Mã bàn đã tồn tại.");
    }

    private AdminRestaurantTableResponse toResponse(RestaurantTable table) {
        Area area = table.getArea();
        return new AdminRestaurantTableResponse(
                table.getId(),
                table.getCode(),
                new AdminTableAreaResponse(area.getId(), area.getName()),
                table.getMinCapacity(),
                table.getMaxCapacity(),
                table.isActive()
        );
    }
}
