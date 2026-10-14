package co.mz.ingenium.entity.UserEntity.ActivityAreaEntity;


import co.mz.ingenium.entity.UserEntity.CompanyEntity.Company;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "activity_area")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ActivityArea {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String typeActivity;
}
