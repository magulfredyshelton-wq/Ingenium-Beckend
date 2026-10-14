package co.mz.ingenium.service.ChatService;

import co.mz.ingenium.call.CallRecordCodec;
import co.mz.ingenium.dto.Authentication.UserType;
import co.mz.ingenium.dto.Chat.ChatActionDTO;
import co.mz.ingenium.dto.Chat.ChatActivityDTO;
import co.mz.ingenium.dto.Chat.ChatAttachmentDTO;
import co.mz.ingenium.dto.Chat.ChatMessageDTO;
import co.mz.ingenium.dto.Chat.ChatReplyDTO;
import co.mz.ingenium.dto.Chat.ConversationDTO;
import co.mz.ingenium.dto.Chat.ResponseMessageDTO;
import co.mz.ingenium.dto.searchUser.UserSummaryDTO;
import co.mz.ingenium.entity.ChatRoomEntity.ChatRoom;
import co.mz.ingenium.entity.MessageEntity.Message;
import co.mz.ingenium.entity.MessageEntity.MessageType;
import co.mz.ingenium.entity.UserEntity.StudentEntity.Students;
import co.mz.ingenium.entity.UserEntity.User;
import co.mz.ingenium.repository.ChatMessageRepository.ChatRoomRepository;
import co.mz.ingenium.repository.ChatMessageRepository.MessageRepository;
import co.mz.ingenium.repository.UserRepository.UserRepository;
import co.mz.ingenium.repository.searchUser.SearchUserRepository;
import co.mz.ingenium.storage.FileVisibility;
import co.mz.ingenium.storage.StorageService;
import co.mz.ingenium.storage.StoredFile;
import co.mz.ingenium.storage.StoredFileRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class MessagingService {

    private static final Duration DOWNLOAD_VALIDITY =
            Duration.ofMinutes(10);

    private final ChatRoomRepository chatRoomRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final SearchUserRepository searchUserRepository;
    private final StorageService storage;
    private final StoredFileRepository files;

    public MessagingService(
            ChatRoomRepository chatRoomRepository,
            MessageRepository messageRepository,
            UserRepository userRepository,
            SearchUserRepository searchUserRepository,
            StorageService storage,
            StoredFileRepository files) {
        this.chatRoomRepository = chatRoomRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.searchUserRepository = searchUserRepository;
        this.storage = storage;
        this.files = files;
    }

    @Transactional
    public ResponseMessageDTO sendMessage(
            UUID senderId,
            ChatMessageDTO dto) {
        if (senderId == null
                || dto == null
                || dto.recipientId() == null) {
            throw new IllegalArgumentException(
                    "Remetente e destinatário são obrigatórios.");
        }

        UUID recipientId = dto.recipientId();

        if (senderId.equals(recipientId)) {
            throw new IllegalArgumentException(
                    "Não podes enviar mensagem a ti próprio!");
        }

        if (!userRepository.existsById(recipientId)) {
            throw new IllegalArgumentException(
                    "Destinatário não encontrado!");
        }

        boolean hasText =
                dto.message() != null && !dto.message().isBlank();

        if (!hasText && dto.fileId() == null) {
            throw new IllegalArgumentException(
                    "Escreve uma mensagem ou anexa um ficheiro.");
        }

        MessageType type = MessageType.TEXT;
        UUID fileId = null;

        if (dto.fileId() != null) {
            StoredFile file = files.findById(dto.fileId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Ficheiro não encontrado!"));

            if (!senderId.equals(file.getOwnerId())
                    || file.getVisibility() != FileVisibility.PRIVATE) {
                throw new IllegalArgumentException("Ficheiro inválido.");
            }

            if (messageRepository.existsByFileId(file.getId())) {
                throw new IllegalArgumentException(
                        "Este ficheiro já foi enviado.");
            }

            type = typeOf(file.getContentType());
            fileId = file.getId();
        }

        ChatRoom room = chatRoomRepository
                .findBetween(senderId, recipientId)
                .orElse(null);

        if (dto.replyToMessageId() != null) {
            if (room == null) {
                throw new IllegalArgumentException(
                        "A mensagem a responder não pertence a esta conversa.");
            }

            Message original = messageRepository.findByIdAndRoomId(
                            dto.replyToMessageId(), room.getId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "A mensagem a responder não pertence a esta conversa."));

            if (original.getDeletedAt() != null) {
                throw new IllegalArgumentException(
                        "Não podes responder a uma mensagem apagada.");
            }
        }

        if (room == null) {
            ChatRoom novaSala = new ChatRoom();
            novaSala.setUser1Id(senderId);
            novaSala.setUser2Id(recipientId);
            novaSala.setCreatedAt(LocalDateTime.now());
            room = chatRoomRepository.save(novaSala);
        }

        Message message = new Message();
        message.setMessage(hasText ? dto.message() : null);
        message.setType(type);
        message.setFileId(fileId);
        message.setReplyToMessageId(dto.replyToMessageId());
        message.setSenderId(senderId);
        message.setSentAt(LocalDateTime.now());
        message.setRoom(room);

        return toDto(
                messageRepository.save(message),
                room.getId());
    }

    @Transactional
    public List<ResponseMessageDTO> getHistoryConversation(
            UUID userId,
            UUID otherUserId) {
        if (userId == null || otherUserId == null) {
            throw new IllegalArgumentException(
                    "Os participantes da conversa são obrigatórios.");
        }

        return chatRoomRepository.findBetween(userId, otherUserId)
                .map(room -> {
                    List<Message> history = messageRepository
                            .findByRoomIdOrderBySentAtAsc(room.getId());

                    Map<UUID, Message> byId = new HashMap<>();
                    history.forEach(message ->
                            byId.put(message.getId(), message));

                    return history.stream()
                            .map(message ->
                                    toDto(message, room.getId(), byId))
                            .toList();
                })
                .orElse(List.of());
    }

    @Transactional
    public List<ConversationDTO> getConversations(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException(
                    "Utilizador não encontrado!");
        }

        return chatRoomRepository
                .findByUser1IdOrUser2Id(userId, userId)
                .stream()
                .map(room -> {
                    UUID otherId =
                            userId.equals(room.getUser1Id())
                                    ? room.getUser2Id()
                                    : room.getUser1Id();

                    Optional<Message> last = messageRepository
                            .findFirstByRoomIdOrderBySentAtDesc(
                                    room.getId());

                    User other = userRepository.findById(otherId)
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "Usuário não encontrado!"));

                    UserType type =
                            other instanceof Students
                                    ? UserType.STUDENT
                                    : UserType.COMPANY;

                    String name = other.getDisplayName();
                    if (name == null || name.isBlank()) {
                        name = "Utilizador";
                    }

                    return new ConversationDTO(
                            room.getId(),
                            otherId,
                            name,
                            type,
                            last.map(MessagingService::preview)
                                    .orElse(""),
                            last.map(Message::getSentAt)
                                    .orElse(room.getCreatedAt()),
                            avatarUrl(other));
                })
                .sorted(Comparator.comparing(
                        ConversationDTO::lastMessageAt,
                        Comparator.nullsLast(
                                Comparator.reverseOrder())))
                .toList();
    }

    public List<UserSummaryDTO> searchUsers(
            String q,
            UUID currentUserId) {
        if (q == null || currentUserId == null) {
            return List.of();
        }

        String term = q.trim();
        if (term.length() < 2) return List.of();

        return searchUserRepository
                .findTop20ByFullNameContainingIgnoreCaseAndIdNotOrderByFullNameAsc(
                        term, currentUserId)
                .stream()
                .map(s -> new UserSummaryDTO(
                        s.getId(),
                        s.getFullName()))
                .toList();
    }

    public Optional<ChatActivityDTO.Event> activity(
            UUID senderId,
            ChatActivityDTO.Request request) {
        if (senderId == null
                || request == null
                || request.recipientId() == null
                || request.state() == null
                || senderId.equals(request.recipientId())) {
            return Optional.empty();
        }

        return chatRoomRepository
                .findBetween(senderId, request.recipientId())
                .filter(room ->
                        participates(room, senderId)
                                && participates(
                                room, request.recipientId()))
                .map(room -> new ChatActivityDTO.Event(
                        room.getId(),
                        senderId,
                        request.recipientId(),
                        request.state(),
                        Instant.now(),
                        request.state() == ChatActivityDTO.State.IDLE
                                ? 0 : 6000));
    }

    @Transactional
    public ChatActionDTO.ReadReceipt markRead(
            UUID readerId,
            UUID otherUserId,
            ChatActionDTO.ReadRequest request) {
        if (readerId == null
                || otherUserId == null
                || request == null
                || request.messageIds() == null
                || request.messageIds().size() > 200
                || request.messageIds().stream()
                .anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(
                    "Mensagens de leitura inválidas.");
        }

        ChatRoom room = chatRoomRepository
                .findBetween(readerId, otherUserId)
                .orElse(null);

        LocalDateTime now = LocalDateTime.now();

        if (room == null || request.messageIds().isEmpty()) {
            return new ChatActionDTO.ReadReceipt(
                    room == null ? null : room.getId(),
                    List.of(),
                    readerId,
                    now);
        }

        List<UUID> ids = new ArrayList<>(
                new LinkedHashSet<>(request.messageIds()));

        List<Message> messages =
                messageRepository.findAllForUpdate(ids);

        if (messages.size() != ids.size()
                || messages.stream().anyMatch(message ->
                message.getRoom() == null
                        || !room.getId().equals(
                        message.getRoom().getId())
                        || !otherUserId.equals(
                        message.getSenderId()))) {
            throw new IllegalArgumentException(
                    "Só podes confirmar mensagens recebidas nesta conversa.");
        }

        List<UUID> read = new ArrayList<>();

        for (Message message : messages) {
            if (message.getReadAt() == null
                    && message.getDeletedAt() == null) {
                message.setReadAt(now);
                messageRepository.save(message);
                read.add(message.getId());
            }
        }

        return new ChatActionDTO.ReadReceipt(
                room.getId(),
                read,
                readerId,
                now);
    }

    @Transactional
    public ResponseMessageDTO deleteMessage(
            UUID userId,
            UUID messageId) {
        Message message =
                authorizedMessage(userId, messageId, true);

        if (!userId.equals(message.getSenderId())) {
            throw new IllegalArgumentException(
                    "Só podes apagar as mensagens que enviaste.");
        }

        if (message.getDeletedAt() == null) {
            message.setDeletedAt(LocalDateTime.now());
            message.setMessage("Mensagem apagada");
            message.setType(MessageType.TEXT);
            message.setFileId(null);
            message.setReplyToMessageId(null);
            messageRepository.save(message);
        }

        return toDto(
                message,
                message.getRoom().getId());
    }

    @Transactional
    public ChatActionDTO.MessageInfo messageInfo(
            UUID userId,
            UUID messageId) {
        Message message =
                authorizedMessage(userId, messageId, false);

        return new ChatActionDTO.MessageInfo(
                message.getId(),
                message.getSenderId(),
                message.getSentAt(),
                message.getReadAt(),
                message.getDeletedAt(),
                message.isForwarded());
    }

    @Transactional
    public ResponseMessageDTO forwardMessage(
            UUID senderId,
            UUID messageId,
            UUID recipientId) {
        Message source =
                authorizedMessage(senderId, messageId, true);

        if (source.getDeletedAt() != null) {
            throw new IllegalArgumentException(
                    "Não podes encaminhar uma mensagem apagada.");
        }

        if (source.getType() == MessageType.CALL) {
            throw new IllegalArgumentException(
                    "Não é possível encaminhar registos de chamadas.");
        }

        if (recipientId == null
                || recipientId.equals(senderId)
                || !userRepository.existsById(recipientId)) {
            throw new IllegalArgumentException(
                    "Destinatário inválido.");
        }

        if (source.getFileId() != null) {
            StoredFile file = files.findById(source.getFileId())
                    .filter(stored ->
                            stored.getVisibility()
                                    == FileVisibility.PRIVATE)
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "O anexo desta mensagem já não está disponível."));

            if (file.getStorageKey() == null
                    || file.getStorageKey().isBlank()) {
                throw new IllegalArgumentException(
                        "Anexo inválido.");
            }
        }

        ChatRoom target = chatRoomRepository
                .findBetween(senderId, recipientId)
                .orElseGet(() -> {
                    ChatRoom room = new ChatRoom();
                    room.setUser1Id(senderId);
                    room.setUser2Id(recipientId);
                    room.setCreatedAt(LocalDateTime.now());
                    return chatRoomRepository.save(room);
                });

        Message message = new Message();
        message.setRoom(target);
        message.setSenderId(senderId);
        message.setSentAt(LocalDateTime.now());
        message.setMessage(source.getMessage());
        message.setType(source.getType());
        message.setFileId(source.getFileId());
        message.setForwarded(true);

        return toDto(
                messageRepository.save(message),
                target.getId());
    }

    @Transactional
    public UUID otherParticipant(
            UUID userId,
            UUID messageId) {
        ChatRoom room = authorizedMessage(
                userId, messageId, false).getRoom();

        return userId.equals(room.getUser1Id())
                ? room.getUser2Id()
                : room.getUser1Id();
    }

    private Message authorizedMessage(
            UUID userId,
            UUID messageId,
            boolean lock) {
        Message message =
                (lock
                        ? messageRepository.findForUpdate(messageId)
                        : messageRepository.findById(messageId))
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Mensagem não encontrada."));

        if (userId == null
                || !participates(message.getRoom(), userId)) {
            throw new IllegalArgumentException(
                    "Mensagem não encontrada.");
        }

        return message;
    }

    private static boolean participates(
            ChatRoom room,
            UUID userId) {
        return room != null
                && userId != null
                && (userId.equals(room.getUser1Id())
                || userId.equals(room.getUser2Id()));
    }

    @Transactional
    public ResponseMessageDTO recordCall(
            UUID roomId,
            UUID callerId,
            String payload) {
        if (roomId == null
                || callerId == null
                || payload == null
                || payload.isBlank()) {
            throw new IllegalArgumentException(
                    "Dados da chamada incompletos.");
        }

        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Conversa não encontrada!"));

        if (!callerId.equals(room.getUser1Id())
                && !callerId.equals(room.getUser2Id())) {
            throw new IllegalArgumentException(
                    "O remetente não participa nesta conversa.");
        }

        Message message = new Message();
        message.setMessage(payload);
        message.setType(MessageType.CALL);
        message.setSenderId(callerId);
        message.setSentAt(LocalDateTime.now());
        message.setRoom(room);

        return toDto(
                messageRepository.save(message),
                room.getId());
    }

    private String avatarUrl(User user) {
        if (user.getAvatarFileId() == null) return null;

        return files.findById(user.getAvatarFileId())
                .filter(f ->
                        f.getVisibility() == FileVisibility.PUBLIC)
                .filter(f ->
                        f.getStorageKey() != null
                                && !f.getStorageKey().isBlank())
                .map(f -> storage.publicUrl(f.getStorageKey()))
                .orElse(null);
    }

    private ResponseMessageDTO toDto(
            Message message,
            UUID roomId) {
        return toDto(message, roomId, null);
    }

    private ResponseMessageDTO toDto(
            Message message,
            UUID roomId,
            Map<UUID, Message> history) {
        if (message.getDeletedAt() != null) {
            return new ResponseMessageDTO(
                    message.getId(),
                    roomId,
                    message.getSenderId(),
                    message.getSentAt(),
                    "Mensagem apagada",
                    MessageType.TEXT,
                    null,
                    null,
                    message.getReadAt(),
                    message.getDeletedAt(),
                    message.isForwarded());
        }

        ChatAttachmentDTO attachment = null;

        if (message.getFileId() != null) {
            attachment = files.findById(message.getFileId())
                    .map(f -> new ChatAttachmentDTO(
                            f.getId(),
                            f.getOriginalName(),
                            f.getContentType(),
                            f.getSizeBytes(),
                            f.getDurationSeconds(),
                            storage.presignedGetPrivate(
                                    f.getStorageKey(),
                                    DOWNLOAD_VALIDITY)))
                    .orElse(null);
        }

        return new ResponseMessageDTO(
                message.getId(),
                roomId,
                message.getSenderId(),
                message.getSentAt(),
                message.getMessage(),
                message.getType(),
                attachment,
                replyOf(message, roomId, history),
                message.getReadAt(),
                message.getDeletedAt(),
                message.isForwarded());
    }

    private ChatReplyDTO replyOf(
            Message message,
            UUID roomId,
            Map<UUID, Message> history) {
        UUID replyId = message.getReplyToMessageId();
        if (replyId == null) return null;

        Message original = history == null
                ? messageRepository.findByIdAndRoomId(
                replyId, roomId).orElse(null)
                : history.get(replyId);

        if (original == null) return null;

        if (original.getDeletedAt() != null) {
            return new ChatReplyDTO(
                    original.getId(),
                    original.getSenderId(),
                    null,
                    MessageType.TEXT,
                    null,
                    true);
        }

        String name = original.getFileId() == null
                ? null
                : files.findById(original.getFileId())
                .map(StoredFile::getOriginalName)
                .orElse(null);

        String text = original.getType() == MessageType.CALL
                ? CallRecordCodec.preview(original.getMessage())
                : original.getMessage();

        return new ChatReplyDTO(
                original.getId(),
                original.getSenderId(),
                text,
                original.getType(),
                name,
                false);
    }

    private static MessageType typeOf(String contentType) {
        if (contentType != null
                && contentType.startsWith("image/")) {
            return MessageType.IMAGE;
        }

        if (contentType != null
                && contentType.startsWith("audio/")) {
            return MessageType.AUDIO;
        }

        return MessageType.FILE;
    }

    private static String preview(Message message) {
        if (message.getDeletedAt() != null) {
            return "Mensagem apagada";
        }

        if (message.getType() == MessageType.CALL) {
            return CallRecordCodec.preview(
                    message.getMessage());
        }

        if (message.getMessage() != null
                && !message.getMessage().isBlank()) {
            return message.getMessage();
        }

        if (message.getType() == null) return "";

        return switch (message.getType()) {
            case IMAGE -> "Imagem";
            case AUDIO -> "Áudio";
            case FILE -> "Ficheiro";
            case CALL -> "Chamada";
            case TEXT -> "";
        };
    }
}