package co.mz.ingenium.dto.Chat;

import co.mz.ingenium.dto.Authentication.UserType;

import java.time.LocalDateTime;
import java.util.UUID;

public record ConversationDTO(
        UUID roomId,
        UUID otherUserId,
        String otherUserName,
        UserType otherUserType,
        String lastMessage,
        LocalDateTime lastMessageAt,
        String otherUserAvatarUrl) {}