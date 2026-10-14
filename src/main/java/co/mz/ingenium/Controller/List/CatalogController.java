package co.mz.ingenium.Controller.List;


import co.mz.ingenium.dto.List.OptionDTO;
import co.mz.ingenium.entity.UserEntity.CourseEntity.Course;
import co.mz.ingenium.repository.ActivityAreaRepository.ActivityAreaRepository;
import co.mz.ingenium.repository.CourseRepository.CourseRepository;
import co.mz.ingenium.repository.FieldStudyRepository.FieldStudyRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/catalog")
public class CatalogController {
    FieldStudyRepository fieldStudyRepository;
    ActivityAreaRepository activityAreaRepository;
    CourseRepository courseRepository;

    public CatalogController(FieldStudyRepository fieldStudyRepository, ActivityAreaRepository activityAreaRepository, CourseRepository courseRepository) {
        this.fieldStudyRepository = fieldStudyRepository;
        this.activityAreaRepository = activityAreaRepository;
        this.courseRepository = courseRepository;
    }

    @GetMapping("/fields")
    public List<OptionDTO> fields() {
        return fieldStudyRepository.findAll()
                .stream()
                .map(f -> new OptionDTO(f.getId(), f.getName()))
                .toList();
    }

    @GetMapping("/fields/{id}/courses")
    public List<OptionDTO> courses(@PathVariable UUID id) {
        return courseRepository.findByFieldStudyId(id)
                .stream()
                .map(c -> new OptionDTO(c.getId(), c.getName()))
                .toList();
    }

    @GetMapping("/activity-area")
    public List<OptionDTO> area() {
        return activityAreaRepository.findAll()
                .stream()
                .map(a -> new OptionDTO(a.getId(), a.getTypeActivity()))
                .toList();
    }
}
