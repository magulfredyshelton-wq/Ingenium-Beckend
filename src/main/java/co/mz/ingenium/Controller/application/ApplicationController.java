package co.mz.ingenium.Controller.application;

import co.mz.ingenium.dto.application.ApplicationDtos.ApplyRequest;
import co.mz.ingenium.dto.application.ApplicationDtos.Candidate;
import co.mz.ingenium.dto.application.ApplicationDtos.Mine;
import co.mz.ingenium.dto.application.ApplicationDtos.MyJob;
import co.mz.ingenium.dto.application.ApplicationDtos.StatusRequest;
import co.mz.ingenium.entity.UserEntity.User;
import co.mz.ingenium.service.application.ApplicationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class ApplicationController {

    private final ApplicationService service;

    public ApplicationController(ApplicationService service) {
        this.service = service;
    }

    // estudante

    @PostMapping("/jobs/{postId}/apply")
    public ResponseEntity<Void> apply(
            @AuthenticationPrincipal User user,
            @PathVariable UUID postId,
            @RequestBody(required = false) ApplyRequest body
    ) {
        service.apply(user.getId(), postId, body == null ? null : body.message());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/jobs/{postId}/apply")
    public ResponseEntity<Void> withdraw(
            @AuthenticationPrincipal User user,
            @PathVariable UUID postId
    ) {
        service.withdraw(user.getId(), postId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/applications/mine")
    public List<Mine> mine(@AuthenticationPrincipal User user) {
        return service.mine(user.getId());
    }

    // empresa

    @GetMapping("/jobs/mine")
    public List<MyJob> myJobs(@AuthenticationPrincipal User user) {
        return service.myJobs(user.getId());
    }

    @GetMapping("/jobs/{postId}/applications")
    public List<Candidate> candidates(
            @AuthenticationPrincipal User user,
            @PathVariable UUID postId
    ) {
        return service.candidates(user.getId(), postId);
    }

    @PostMapping("/applications/{id}/status")
    public ResponseEntity<Void> setStatus(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id,
            @RequestBody StatusRequest body
    ) {
        service.setStatus(user.getId(), id, body.status());
        return ResponseEntity.noContent().build();
    }
}