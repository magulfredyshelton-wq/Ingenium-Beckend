package co.mz.ingenium.config.videocall;

import co.mz.ingenium.repository.ChatMessageRepository.ChatRoomRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Decide quem pode entrar numa sala de chamada.
 * A sala de chamada é a própria conversa (ChatRoom): o nome da sala é o id da conversa,
 * e só os dois participantes dessa conversa podem entrar.
 */
@Service
public class CallAccessService {

    private final ChatRoomRepository rooms;

    public CallAccessService(ChatRoomRepository rooms) {
        this.rooms = rooms;
    }

    public boolean canJoin(UUID userId, String room) {
        UUID roomId;
        try {
            roomId = UUID.fromString(room);
        } catch (IllegalArgumentException e) {
            return false; // o nome da sala não é um id de conversa
        }
        return rooms.findById(roomId)
                .map(r -> userId.equals(r.getUser1Id()) || userId.equals(r.getUser2Id()))
                .orElse(false);
    }

    /** O outro participante da conversa; vazio se a conversa não existe ou o utilizador não faz parte dela. */
    public Optional<UUID> otherParticipant(UUID roomId, UUID userId) {
        return rooms.findById(roomId).flatMap(r -> {
            if (userId.equals(r.getUser1Id())) {
                return Optional.ofNullable(r.getUser2Id());
            }
            if (userId.equals(r.getUser2Id())) {
                return Optional.ofNullable(r.getUser1Id());
            }
            return Optional.empty();
        });
    }
}