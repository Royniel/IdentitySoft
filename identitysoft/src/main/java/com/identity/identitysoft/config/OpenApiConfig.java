package com.identity.identitysoft.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

@OpenAPIDefinition(
        info = @Info(
                title = "IdentitySoft API",
                version = "v1",
                description = "JWT-secured identity management API: registration, login, password reset, "
                        + "and admin user management."
        )
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Paste the accessToken returned by /api/auth/login or /api/auth/register "
                + "(just the token itself — Swagger UI adds the \"Bearer \" prefix for you)."
)
@Configuration
public class OpenApiConfig {
}
