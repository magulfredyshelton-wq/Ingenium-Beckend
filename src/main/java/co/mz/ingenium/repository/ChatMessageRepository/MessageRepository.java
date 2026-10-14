package co.mz.ingenium.repository.ChatMessageRepository;

import co.mz.ingenium.entity.MessageEntity.Message;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MessageRepository
        extends JpaRepository<Message, UUID> {

    List<Message> findByRoomIdOrderBySentAtAsc(UUID roomId);

    Optional<Message> findFirstByRoomIdOrderBySentAtDesc(UUID roomId);

    Optional<Message> findByIdAndRoomId(UUID id, UUID roomId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Message m where m.id = :id")
    Optional<Message> findForUpdate(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Message m where m.id in :ids order by m.id")
    List<Message> findAllForUpdate(@Param("ids") List<UUID> ids);

    boolean existsByFileId(UUID fileId);
}