package co.mz.ingenium.dto.profile;

/** Contactos públicos de um perfil. Qualquer campo pode ser null. */
public record ContactDTO(
        String phone,
        String email,
        String website,
        String linkedin,
        String github,
        String location
) {}