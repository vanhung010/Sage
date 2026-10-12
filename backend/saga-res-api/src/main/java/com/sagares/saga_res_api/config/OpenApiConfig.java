package com.sagares.saga_res_api.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Saga Res API",
                version = "1.0",
                description = "Current HTTP API for the Saga Res restaurant backend. Only endpoints implemented in the source code are exposed here."
        )
)
@SecurityScheme(
        name = "bearerAuth",
        description = "JWT access token returned by POST /api/auth/login.",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class OpenApiConfig {
}
