package co.mz.ingenium.dto.GoogleOAuth2;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record GoogleAuthDTO(
        @NotBlank String credential,
        UUID courseId) {}