package co.mz.ingenium.Controller.Verification;

import co.mz.ingenium.service.Verification.EmailService;
import co.mz.ingenium.service.Verification.VerificationCodeService;
import co.mz.ingenium.service.Verification.VerificationPurpose;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class VerificationController {

    private final VerificationCodeService codes;
    private final EmailService email;

    public VerificationController(VerificationCodeService codes, EmailService email) {
        this.codes = codes;
        this.email = email;
    }

    public record SendCodeRequest(String email) {}
    public record VerifyCodeRequest(String email, String code) {}

    @PostMapping("/send-code")
    public ResponseEntity<Void> send(@RequestBody SendCodeRequest req) {
        String code = codes.issue(req.email(), VerificationPurpose.REGISTER);
        email.sendRegistrationCode(req.email(), code);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/verify-code")
    public ResponseEntity<Void> verify(@RequestBody VerifyCodeRequest req) {
        codes.verify(req.email(), VerificationPurpose.REGISTER, req.code());
        return ResponseEntity.noContent().build();
    }
}