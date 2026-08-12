package com.identity.identitysoft.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ForgotPasswordRequest(
        @NotBlank(message = "Username or email is required")
        String identifier,

        @NotBlank(message = "New password is required")
        @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9]).{8,}$",
                message = "Password must be at least 8 characters and include an uppercase letter, a number, and a special character"
        )
        String newPassword,

        @NotBlank(message = "Please confirm your new password")
        String confirmPassword
) {}
