package co.mz.ingenium.dto.Authentication;

import co.mz.ingenium.service.Verification.VerificationPurpose;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthVerificationDtos {

    private AuthVerificationDtos() {}

    public record Request(
            @NotBlank @Email @Size(max = 255) String email,
            @NotNull VerificationPurpose purpose) {}

    public record Reset(
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank
            @Pattern(
                    regexp = "[0-9]{6}",
                    message = "O código deve ter 6 dígitos.")
            String code,
            @NotBlank @Size(min = 8, max = 72)
            String newPassword) {}

    public record Message(String message) {}
}