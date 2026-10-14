package co.mz.ingenium.entity.UserEntity.CompanyEntity;

import co.mz.ingenium.entity.UserEntity.ActivityAreaEntity.ActivityArea;
import co.mz.ingenium.entity.UserEntity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "company")
@DiscriminatorValue("COMPANY")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Company extends User {
    private String companyName;
    private String corporateName;
    @ManyToOne
    @JoinColumn(name = "activity_area_id")
    private ActivityArea activityArea;

    @Override
    public String getDisplayName() {
        return companyName;
    }
}
