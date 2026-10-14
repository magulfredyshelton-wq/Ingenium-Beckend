package co.mz.ingenium.repository.ActivityAreaRepository;

import co.mz.ingenium.entity.UserEntity.ActivityAreaEntity.ActivityArea;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ActivityAreaRepository extends JpaRepository<ActivityArea, UUID> {
}
