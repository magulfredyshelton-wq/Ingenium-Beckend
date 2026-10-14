package co.mz.ingenium.service.AuthService;

import co.mz.ingenium.dto.Authentication.AuthVerificationDtos;
import co.mz.ingenium.dto.Authentication.Login.LoginRequest;
import co.mz.ingenium.dto.Authentication.Register.RegisterCompanyDTO;
import co.mz.ingenium.dto.Authentication.Register.RegisterStudentDTO;
import co.mz.ingenium.dto.Authentication.Response;
import co.mz.ingenium.dto.Authentication.UserType;
import co.mz.ingenium.dto.GoogleOAuth2.GoogleAuthDTO;
import co.mz.ingenium.dto.GoogleOAuth2.GoogleUserDTO;
import co.mz.ingenium.entity.UserEntity.ActivityAreaEntity.ActivityArea;
import co.mz.ingenium.entity.UserEntity.CompanyEntity.Company;
import co.mz.ingenium.entity.UserEntity.CourseEntity.Course;
import co.mz.ingenium.entity.UserEntity.StudentEntity.Students;
import co.mz.ingenium.entity.UserEntity.User;
import co.mz.ingenium.repository.ActivityAreaRepository.ActivityAreaRepository;
import co.mz.ingenium.repository.CourseRepository.CourseRepository;
import co.mz.ingenium.repository.UserRepository.UserRepository;
import co.mz.ingenium.service.GoogleTokenVerifier;
import co.mz.ingenium.service.JWTService.JWTService;
import co.mz.ingenium.service.Verification.EmailService;
import co.mz.ingenium.service.Verification.VerificationCodeService;
import co.mz.ingenium.service.Verification.VerificationPurpose;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.Period;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

@Service
public class AuthService {

    private static final Pattern USERNAME =
            Pattern.compile("^[a-z0-9_.]{3,30}$");

    private static final Set<String> RESERVED = Set.of(
            "admin", "administrador", "ingenium", "suporte",
            "support", "moderador", "me", "api", "root",
            "null", "undefined", "ajuda", "help");

    public record UsernameCheck(boolean available, String message) {}

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CourseRepository courseRepository;
    private final ActivityAreaRepository activityAreaRepository;
    private final JWTService jwt;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final VerificationCodeService codes;
    private final EmailService emails;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            CourseRepository courseRepository,
            ActivityAreaRepository activityAreaRepository,
            JWTService jwt,
            GoogleTokenVerifier googleTokenVerifier,
            VerificationCodeService codes,
            EmailService emails) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.courseRepository = courseRepository;
        this.activityAreaRepository = activityAreaRepository;
        this.jwt = jwt;
        this.googleTokenVerifier = googleTokenVerifier;
        this.codes = codes;
        this.emails = emails;
    }

    public UsernameCheck checkUsername(String raw) {
        String username = normalizeUsername(raw);

        if (!USERNAME.matcher(username).matches()) {
            return new UsernameCheck(false,
                    "Usa 3 a 30 caracteres: letras minúsculas, números, _ e .");
        }

        if (RESERVED.contains(username)
                || userRepository.existsByUsername(username)) {
            return new UsernameCheck(false,
                    "Este nome de utilizador já está em uso.");
        }

        return new UsernameCheck(true, null);
    }

    private String requireFreeUsername(String raw) {
        UsernameCheck check = checkUsername(raw);
        if (!check.available()) {
            throw new IllegalArgumentException(check.message());
        }
        return normalizeUsername(raw);
    }

    private static String normalizeUsername(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeEmail(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
    }

    private static void requireValidPassword(String password) {
        if (password == null
                || password.isBlank()
                || password.length() < 8) {
            throw new IllegalArgumentException(
                    "A palavra-passe deve ter pelo menos 8 caracteres.");
        }

        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException(
                    "A palavra-passe é demasiado longa. Reduza o tamanho.");
        }
    }

    private String generateUsername(String base) {
        String slug = Normalizer.normalize(
                        base == null ? "" : base,
                        Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");

        if (slug.length() < 3) slug = "user";
        if (slug.length() > 22) slug = slug.substring(0, 22);

        String candidate = slug;
        while (RESERVED.contains(candidate)
                || userRepository.existsByUsername(candidate)) {
            candidate =
                    slug + ThreadLocalRandom.current().nextInt(100, 10000);
        }
        return candidate;
    }

    public AuthVerificationDtos.Message requestVerification(
            AuthVerificationDtos.Request dto) {

        String email = normalizeEmail(dto.email());
        boolean exists = userRepository.existsByEmailIgnoreCase(email);

        if (dto.purpose() == VerificationPurpose.REGISTER && exists) {
            throw new IllegalArgumentException("Email já cadastrado!");
        }

        String code = codes.issue(email, dto.purpose());

        if (dto.purpose() == VerificationPurpose.REGISTER) {
            emails.sendRegistrationCode(email, code);
            return new AuthVerificationDtos.Message(
                    "Pedido recebido. Consulte o seu email para obter o código.");
        }

        if (exists) {
            emails.sendPasswordResetCode(email, code);
        }

        return new AuthVerificationDtos.Message(
                "Se existir uma conta com esse email, "
                        + "receberá um código de recuperação.");
    }

    @Transactional
    public Response registerStudent(RegisterStudentDTO dto) {
        String email = normalizeEmail(dto.email());

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Email já cadastrado!");
        }

        String username = requireFreeUsername(dto.username());
        requireValidPassword(dto.password());

        if (dto.birthDate() == null
                || Period.between(
                dto.birthDate(), LocalDate.now()).getYears() < 16) {
            throw new IllegalArgumentException("Idade mínima: 16 anos");
        }

        Course course = courseRepository.findById(dto.courseId())
                .orElseThrow(() ->
                        new IllegalArgumentException("Curso não encontrado!"));

        codes.verify(
                email,
                VerificationPurpose.REGISTER,
                dto.verificationCode());

        Students student = new Students();
        student.setEmail(email);
        student.setUsername(username);
        student.setPassword(passwordEncoder.encode(dto.password()));
        student.setFullName(dto.fullName());
        student.setBirthDate(dto.birthDate());
        student.setCourse(course);
        userRepository.save(student);

        return new Response(
                jwt.generateToken(student.getEmail()),
                student.getId(),
                UserType.STUDENT);
    }

    @Transactional
    public Response registerCompany(RegisterCompanyDTO dto) {
        String email = normalizeEmail(dto.email());

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Email já cadastrado!");
        }

        String username = requireFreeUsername(dto.username());
        requireValidPassword(dto.password());

        ActivityArea activityArea =
                activityAreaRepository.findById(dto.activityAreaId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Sector de actividade não encontrado!"));

        codes.verify(
                email,
                VerificationPurpose.REGISTER,
                dto.verificationCode());

        Company company = new Company();
        company.setEmail(email);
        company.setUsername(username);
        company.setPassword(passwordEncoder.encode(dto.password()));
        company.setCompanyName(dto.companyName());
        company.setCorporateName(dto.corporateName());
        company.setActivityArea(activityArea);
        userRepository.save(company);

        return new Response(
                jwt.generateToken(company.getEmail()),
                company.getId(),
                UserType.COMPANY);
    }

    public Response login(LoginRequest request) {
        User user = userRepository
                .findByEmailIgnoreCase(normalizeEmail(request.email()))
                .orElseThrow(() -> new BadCredentialsException(
                        "Usuário ou senha incorrecta!"));

        if (!passwordEncoder.matches(
                request.password(), user.getPassword())) {
            throw new BadCredentialsException(
                    "Usuário ou senha incorrecta!");
        }

        UserType type =
                user instanceof Students
                        ? UserType.STUDENT : UserType.COMPANY;

        return new Response(
                jwt.generateToken(user.getEmail()),
                user.getId(),
                type);
    }

    @Transactional
    public AuthVerificationDtos.Message resetPassword(
            AuthVerificationDtos.Reset dto) {

        requireValidPassword(dto.newPassword());
        String email = normalizeEmail(dto.email());

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Código inválido ou expirado."));

        codes.verify(
                email,
                VerificationPurpose.PASSWORD_RESET,
                dto.code());

        user.setPassword(passwordEncoder.encode(dto.newPassword()));
        userRepository.save(user);

        return new AuthVerificationDtos.Message(
                "Palavra-passe alterada. Entre com a nova palavra-passe.");
    }

    @Transactional
    public Response loginWithGoogle(GoogleAuthDTO dto) {
        GoogleUserDTO google =
                googleTokenVerifier.verify(dto.credential());

        User existing = userRepository
                .findByEmailIgnoreCase(google.email())
                .orElse(null);

        if (existing != null) {
            UserType type =
                    existing instanceof Students
                            ? UserType.STUDENT : UserType.COMPANY;

            return new Response(
                    jwt.generateToken(existing.getEmail()),
                    existing.getId(),
                    type);
        }

        if (dto.courseId() == null) {
            throw new IllegalArgumentException(
                    "Escolha primeiro o seu curso!");
        }

        Course course = courseRepository.findById(dto.courseId())
                .orElseThrow(() ->
                        new IllegalArgumentException("Curso não encontrado!"));

        Students student = new Students();
        student.setEmail(google.email());
        student.setUsername(generateUsername(
                google.name() != null
                        ? google.name()
                        : google.email().split("@")[0]));
        student.setPassword(
                passwordEncoder.encode(UUID.randomUUID().toString()));
        student.setFullName(
                google.name() != null ? google.name() : google.email());
        student.setCourse(course);

        userRepository.save(student);

        return new Response(
                jwt.generateToken(student.getEmail()),
                student.getId(),
                UserType.STUDENT);
    }
}