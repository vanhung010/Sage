package com.sagares.saga_res_api.table.service;

import com.sagares.saga_res_api.table.dto.publics.PublicAreaResponse;
import com.sagares.saga_res_api.table.repository.AreaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PublicAreaService {

    private final AreaRepository areaRepository;

    @Transactional(readOnly = true)
    public List<PublicAreaResponse> getAreas() {
        return areaRepository
                .findByActiveTrueOrderByNameAsc()
                .stream()
                .map(area -> new PublicAreaResponse(
                        area.getId(),
                        area.getName(),
                        area.getDescription()
                ))
                .toList();
    }
}