package com.sagares.saga_res_api.menu.service;

import com.sagares.saga_res_api.menu.dto.publicmenu.PublicDishTagResponse;
import com.sagares.saga_res_api.menu.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublicMenuTagService {

    private final TagRepository tagRepository;

    public List<PublicDishTagResponse> listTags() {
        return tagRepository.findAllByOrderByNameAsc().stream()
                .map(tag -> new PublicDishTagResponse(tag.getId(), tag.getName()))
                .toList();
    }
}
