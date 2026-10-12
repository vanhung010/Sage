package com.sagares.saga_res_api.table.service;

import com.sagares.saga_res_api.common.exception.BusinessException;
import com.sagares.saga_res_api.table.dto.admin.AdminAreaResponse;
import com.sagares.saga_res_api.table.dto.admin.CreateAreaRequest;
import com.sagares.saga_res_api.table.dto.admin.UpdateAreaRequest;
import com.sagares.saga_res_api.table.entity.Area;
import com.sagares.saga_res_api.table.repository.AreaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminAreaService {

    private static final int AREA_NAME_MAX_LENGTH = 100;

    private final AreaRepository areaRepository;

    public List<AdminAreaResponse> listAreas() {
        return areaRepository.findAllByOrderByNameAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AdminAreaResponse createArea(CreateAreaRequest request) {
        String name = normalizeName(request.name());
        if (areaRepository.existsByName(name)) {
            throw duplicateAreaName();
        }

        Area area = new Area();
        area.setName(name);
        area.setDescription(request.description() == null ? null : request.description().trim());
        area.setActive(true);

        return toResponse(save(area));
    }

    @Transactional
    public AdminAreaResponse updateArea(Long id, UpdateAreaRequest request) {
        if (request.name() == null && request.description() == null) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "RỖNG",
                    "Ít nhất một trường khu vực phải được cung cấp.."
            );
        }

        Area area = findArea(id);
        if (request.name() != null) {
            String name = normalizeName(request.name());
            if (!name.equals(area.getName()) && areaRepository.existsByNameAndIdNot(name, id)) {
                throw duplicateAreaName();
            }
            area.setName(name);
        }

        if (request.description() != null) {
            area.setDescription(request.description().trim());
        }

        return toResponse(save(area));
    }

    //Tắt bàn
    @Transactional
    public AdminAreaResponse deactivateArea(Long id) {
        Area area = findArea(id);
        area.setActive(false);
        return toResponse(save(area));
    }

    private Area findArea(Long id) {
        return areaRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "AREA_NOT_FOUND",
                        "Khu vực không tìm thấy."
                ));
    }

    private Area save(Area area) {
        try {
            return areaRepository.saveAndFlush(area);
        } catch (DataIntegrityViolationException exception) {
            throw duplicateAreaName();
        }
    }

    private String normalizeName(String name) {
        String normalized = name == null ? "" : name.trim();
        if (normalized.isBlank()) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_AREA_NAME",
                    "Tên khu vực không được để trống."
            );
        }
        if (normalized.length() > AREA_NAME_MAX_LENGTH) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_AREA_NAME",
                    "Tên khu vực tối đa 100 kí tự."
            );
        }
        return normalized;
    }

    private BusinessException duplicateAreaName() {
        return new BusinessException(
                HttpStatus.CONFLICT,
                "AREA_NAME_ALREADY_EXISTS",
                "Area name already exists."
        );
    }

    private AdminAreaResponse toResponse(Area area) {
        return new AdminAreaResponse(
                area.getId(),
                area.getName(),
                area.getDescription(),
                area.isActive()
        );
    }
}
