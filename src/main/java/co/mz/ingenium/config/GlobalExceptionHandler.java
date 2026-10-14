package co.mz.ingenium.config;


import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler (BadCredentialsException.class)
    public ResponseEntity<Map<String, String>> handlerBadCredential(BadCredentialsException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", String.valueOf(e.getMessage())));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handlerBadRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error",  String.valueOf(e.getMessage())));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handlerValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(Map.of("error", msg));
    }

    /** Uma regra da base de dados foi violada: devolve 400 com mensagem em vez de 500 mudo. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handlerIntegrity(DataIntegrityViolationException e) {
        System.err.println("Violação de integridade: " + e.getMostSpecificCause().getMessage());
        return ResponseEntity.badRequest().body(Map.of("error",
                "Os dados enviados não cumprem as regras da plataforma. Confirma os campos e tenta de novo."));
    }
}
