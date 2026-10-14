package co.mz.ingenium.Controller.jobs;

import co.mz.ingenium.dto.jobs.JobDTO;
import co.mz.ingenium.entity.UserEntity.User;
import co.mz.ingenium.service.jobs.JobService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/jobs")
public class JobController {

    private final JobService service;

    public JobController(JobService service) {
        this.service = service;
    }

    @GetMapping
    public List<JobDTO> search(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return service.search(user.getId(), query, page, size);
    }
}