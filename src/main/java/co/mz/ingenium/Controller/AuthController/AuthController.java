package co.mz.ingenium.Controller.AuthController;

import co.mz.ingenium.dto.Authentication.AuthVerificationDtos;
import co.mz.ingenium.dto.Authentication.Login.LoginRequest;
import co.mz.ingenium.dto.Authentication.Register.RegisterCompanyDTO;
import co.mz.ingenium.dto.Authentication.Register.RegisterStudentDTO;
import co.mz.ingenium.dto.Authentication.Response;
import co.mz.ingenium.dto.GoogleOAuth2.GoogleAuthDTO;
import co.mz.ingenium.service.AuthService.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register/student")
    public ResponseEntity<Response> registerStudent(
            @RequestBody @Valid RegisterStudentDTO dto) {
        return ResponseEntity.ok(authService.registerStudent(dto));
    }

    @PostMapping("/register/company")
    public ResponseEntity<Response> registerCompany(
            @RequestBody @Valid RegisterCompanyDTO dto) {
        return ResponseEntity.ok(authService.registerCompany(dto));
    }

    @PostMapping("/login")
    public ResponseEntity<Response> login(
            @RequestBody @Valid LoginRequest dto) {
        return ResponseEntity.ok(authService.login(dto));
    }

    @PostMapping({"/google", "/auth/google"})
    public Response google(
            @RequestBody @Valid GoogleAuthDTO dto) {
        return authService.loginWithGoogle(dto);
    }

    @GetMapping("/username-available")
    public AuthService.UsernameCheck usernameAvailable(
            @RequestParam String username) {
        return authService.checkUsername(username);
    }

    @PostMapping("/verification/request")
    public AuthVerificationDtos.Message requestVerification(
            @RequestBody @Valid AuthVerificationDtos.Request dto) {
        return authService.requestVerification(dto);
    }

    @PostMapping("/password/reset")
    public AuthVerificationDtos.Message resetPassword(
            @RequestBody @Valid AuthVerificationDtos.Reset dto) {
        return authService.resetPassword(dto);
    }
}