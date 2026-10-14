package co.mz.ingenium.dto.Authentication;

import java.util.UUID;

public record Response (String token, UUID userId, UserType type){
}
