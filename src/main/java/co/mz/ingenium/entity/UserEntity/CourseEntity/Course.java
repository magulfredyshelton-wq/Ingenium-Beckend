package co.mz.ingenium.entity.UserEntity.CourseEntity;

import co.mz.ingenium.entity.UserEntity.FieldStudyEntity.FieldStudy;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "course")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String name;
    @ManyToOne
    @JoinColumn(name = "field_study_id")
    private FieldStudy fieldStudy;
}
