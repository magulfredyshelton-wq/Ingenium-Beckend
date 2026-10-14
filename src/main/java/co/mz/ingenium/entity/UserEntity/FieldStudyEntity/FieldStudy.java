package co.mz.ingenium.entity.UserEntity.FieldStudyEntity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "field_study")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class FieldStudy {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String name;
}
