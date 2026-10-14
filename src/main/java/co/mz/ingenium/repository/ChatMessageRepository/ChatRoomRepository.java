package co.mz.ingenium.repository.ChatMessageRepository;

import co.mz.ingenium.entity.ChatRoomEntity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, UUID> {

    @Query("select r from ChatRoom r where (r.user1Id = :a and r.user2Id = :b) or (r.user1Id = :b and r.user2Id = :a)")
    Optional<ChatRoom> findBetween(@Param("a") UUID a, @Param("b") UUID b);
    List<ChatRoom> findByUser1IdOrUser2Id(UUID a, UUID b);
}
