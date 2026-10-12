package com.sagares.saga_res_api.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

@Schema(description = "Generic page metadata and content. No currently implemented controller returns this type yet.")
public record PageResponse<T>(
        @Schema(description = "Items in the current page.")
        List<T> content,
        @Schema(description = "Zero-based page index.", example = "0", minimum = "0")
        int page,
        @Schema(description = "Requested page size.", example = "20", minimum = "1")
        int size,
        @Schema(description = "Total number of matching items.", example = "42", minimum = "0")
        long totalElements,
        @Schema(description = "Total number of pages.", example = "3", minimum = "0")
        int totalPages,
        @Schema(description = "Whether this is the first page.", example = "true")
        boolean first,
        @Schema(description = "Whether this is the last page.", example = "false")
        boolean last
) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
