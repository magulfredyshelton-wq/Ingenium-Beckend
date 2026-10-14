package co.mz.ingenium.entity.ChatRoomEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "chatroom")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ChatRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private UUID user1Id;
    private UUID user2Id;
    private LocalDateTime createdAt;
}
