package co.mz.ingenium.dto.Authentication.Register;

import jakarta.validation.constraints.*;

import java.util.UUID;

public record RegisterCompanyDTO(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank
        @Pattern(
                regexp = "^[a-z0-9_.]{3,30}$",
                message = "Usa 3 a 30 caracteres: letras minúsculas, números, _ e .")
        String username,
        @NotBlank String companyName,
        @NotBlank String corporateName,
        @NotNull UUID activityAreaId,
        @NotBlank
        @Pattern(
                regexp = "[0-9]{6}",
                message = "O código deve ter 6 dígitos.")
        String verificationCode) {}