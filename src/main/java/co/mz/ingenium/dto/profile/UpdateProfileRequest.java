package co.mz.ingenium.dto.profile;

/**
 * Corpo de PUT /users/me/profile. O cliente envia sempre todos os campos:
 * o que vier vazio ou null é apagado.
 */
public record UpdateProfileRequest(
        String bio,
        String phone,
        String contactEmail,
        String website,
        String linkedin,
        String github,
        String location
) {}