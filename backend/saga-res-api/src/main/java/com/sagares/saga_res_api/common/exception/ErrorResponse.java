package com.sagares.saga_res_api.common.exception;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Standard error response returned by validation, business-rule, authentication, and authorization failures.")
public record ErrorResponse(
        @Schema(description = "HTTP status code.", example = "400")
        int status,

        @Schema(description = "Stable application error code. Business-rule failures may use a more specific code.", example = "VALIDATION_ERROR")
        String code,

        @Schema(description = "Human-readable explanation of the error.", example = "email: must be a well-formed email address")
        String message
) {
}
