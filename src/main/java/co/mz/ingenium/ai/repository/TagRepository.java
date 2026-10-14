package co.mz.ingenium.ai.repository;

import co.mz.ingenium.ai.model.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TagRepository extends JpaRepository<Tag, UUID> {

    // Procura exata pelo slug (ex.: "desenvolvimento-web")
    Optional<Tag> findBySlugAndActiveTrue(String slug);

    // Todas as tags ativas
    List<Tag> findByActiveTrueOrderByNameAsc();
}