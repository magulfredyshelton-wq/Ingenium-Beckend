package co.mz.ingenium.repository.CourseRepository;

import co.mz.ingenium.entity.UserEntity.CourseEntity.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CourseRepository extends JpaRepository<Course, UUID> {
    List<Course> findByFieldStudyId(UUID fieldStudyId);
}