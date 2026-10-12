package com.sagares.saga_res_api.menu.service;

import com.sagares.saga_res_api.common.exception.BusinessException;
import com.sagares.saga_res_api.menu.dto.admin.AdminTagResponse;
import com.sagares.saga_res_api.menu.dto.admin.CreateTagRequest;
import com.sagares.saga_res_api.menu.dto.admin.UpdateTagRequest;
import com.sagares.saga_res_api.menu.entity.Tag;
import com.sagares.saga_res_api.menu.repository.DishRepository;
import com.sagares.saga_res_api.menu.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminMenuTagService {

    private static final int TAG_NAME_MAX_LENGTH = 50;

    private final TagRepository tagRepository;
    private final DishRepository dishRepository;

    public List<AdminTagResponse> listTags() {
        return tagRepository.findAllByOrderByNameAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AdminTagResponse createTag(CreateTagRequest request) {
        String name = normalizeName(request.name());
        if (tagRepository.existsByName(name)) {
            throw duplicateTagName();
        }

        Tag tag = new Tag();
        tag.setName(name);
        return toResponse(save(tag));
    }

    @Transactional
    public AdminTagResponse updateTag(Long id, UpdateTagRequest request) {
        if (request.name() == null) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "EMPTY_PATCH",
                    "Phải cung cấp ít nhất một trường để cập nhật"
            );
        }

        Tag tag = findTag(id);
        String name = normalizeName(request.name());
        if (!name.equals(tag.getName()) && tagRepository.existsByNameAndIdNot(name, id)) {
            throw duplicateTagName();
        }

        tag.setName(name);
        return toResponse(save(tag));
    }

    @Transactional
    public void deleteTag(Long id) {
        Tag tag = findTag(id);
        if (dishRepository.existsByTags_Id(id)) {
            throw tagInUse();
        }

        try {
            tagRepository.delete(tag);
            tagRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw tagInUse();
        }
    }

    private Tag findTag(Long id) {
        return tagRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "TAG_NOT_FOUND",
                        "Nhãn không tồn tại"
                ));
    }

    private Tag save(Tag tag) {
        try {
            return tagRepository.saveAndFlush(tag);
        } catch (DataIntegrityViolationException exception) {
            throw duplicateTagName();
        }
    }

    private String normalizeName(String name) {
        String normalized = name == null ? "" : name.trim();
        if (normalized.isBlank()) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_TAG_NAME",
                    "Tên nhãn không được để trống"
            );
        }
        if (normalized.length() > TAG_NAME_MAX_LENGTH) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_TAG_NAME",
                    "Tên nhãn không được vượt quá 50 ký tự"
            );
        }
        return normalized;
    }

    private BusinessException duplicateTagName() {
        return new BusinessException(
                HttpStatus.CONFLICT,
                "DUPLICATE_TAG_NAME",
                "Tên nhãn đã tồn tại"
        );
    }

    private BusinessException tagInUse() {
        return new BusinessException(
                HttpStatus.CONFLICT,
                "TAG_IN_USE",
                "Không thể xóa nhãn đang được gán cho món ăn"
        );
    }

    private AdminTagResponse toResponse(Tag tag) {
        return new AdminTagResponse(tag.getId(), tag.getName());
    }
}
