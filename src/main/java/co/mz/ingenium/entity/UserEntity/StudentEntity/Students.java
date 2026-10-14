package co.mz.ingenium.entity.UserEntity.StudentEntity;

import co.mz.ingenium.entity.UserEntity.CourseEntity.Course;
import co.mz.ingenium.entity.UserEntity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;


@Entity
@Table(name = "students")
@DiscriminatorValue("STUDENT")
@NoArgsConstructor
@Getter
@Setter
public class Students extends User {
    private String fullName;
    private LocalDate birthDate;
    @ManyToOne
    @JoinColumn(name = "course_id")
    private Course course;

    @Override
    public String getDisplayName() {
        return fullName;
    }

}
