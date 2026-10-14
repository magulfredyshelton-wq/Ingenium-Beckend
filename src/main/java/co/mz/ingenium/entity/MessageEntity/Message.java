package co.mz.ingenium.entity.MessageEntity;

import co.mz.ingenium.entity.ChatRoomEntity.ChatRoom;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "message")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String message;
    private UUID senderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MessageType type = MessageType.TEXT;

    @Column(name = "file_id")
    private UUID fileId;

    @Column(name = "reply_to_message_id")
    private UUID replyToMessageId;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(nullable = false)
    private boolean forwarded;

    private LocalDateTime sentAt;

    @ManyToOne
    @JoinColumn(name = "room_id")
    private ChatRoom room;
}