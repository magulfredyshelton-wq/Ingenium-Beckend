package co.mz.ingenium.repository.FieldStudyRepository;

import co.mz.ingenium.entity.UserEntity.FieldStudyEntity.FieldStudy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FieldStudyRepository extends JpaRepository<FieldStudy, UUID> {
}