package co.mz.ingenium.repository.searchUser;

import co.mz.ingenium.entity.UserEntity.StudentEntity.Students;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SearchUserRepository extends JpaRepository<Students, UUID> {
    List<Students> findTop20ByFullNameContainingIgnoreCaseAndIdNotOrderByFullNameAsc(String q, UUID id);
}