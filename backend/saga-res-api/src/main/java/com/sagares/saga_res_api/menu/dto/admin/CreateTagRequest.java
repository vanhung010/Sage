package com.sagares.saga_res_api.menu.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to create a menu tag.")
public record CreateTagRequest(
        @Schema(description = "Unique tag name. Must be non-blank and at most 50 characters.", example = "Best Seller", maxLength = 50)
        @NotBlank(message = "Tên nhãn không được để trống")
        @Size(max = 50, message = "Tên nhãn không được vượt quá 50 ký tự")
        String name
) {
}
