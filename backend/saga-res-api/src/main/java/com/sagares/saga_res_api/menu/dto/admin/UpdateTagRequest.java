package com.sagares.saga_res_api.menu.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Partial menu tag update. The name field must be supplied.")
public record UpdateTagRequest(
        @Schema(description = "New unique tag name. Must be non-blank after trimming and at most 50 characters.", example = "Chef's Choice", maxLength = 50, nullable = true)
        @Size(max = 50, message = "Tên nhãn không được vượt quá 50 ký tự")
        String name
) {
}
