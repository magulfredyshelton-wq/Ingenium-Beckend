package co.mz.ingenium.ai.exception;

import co.mz.ingenium.ai.dto.ErroResposta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Necessidade: sem isto, qualquer erro devolvia uma página/JSON genérico do Spring, diferente
 * em cada caso. O frontend precisa de erros SEMPRE no mesmo formato.
 * Funções: apanhar as exceções lançadas pelos controllers e convertê-las em ErroResposta JSON
 * com o estado HTTP correto.
 */
// Aplica-se a todos os @RestController; o retorno é escrito como JSON
@RestControllerAdvice(basePackages = "co.mz.ingenium.ai")
public class AiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(AiExceptionHandler.class);

    // Trata todas as exceções do Ingenium (e subclasses)
    @ExceptionHandler(IngeniumException.class)
    public ResponseEntity<ErroResposta> ingenium(IngeniumException e) {
        // Erros 5xx interessam à equipa: ficam no log com a causa
        if (e.getEstado().is5xxServerError()) {
            // Regista a mensagem e a pilha de chamadas
            log.error("Erro do módulo de IA: {}", e.getMessage(), e);
        }
        // Devolve o estado da exceção com o corpo padronizado
        return ResponseEntity.status(e.getEstado()).body(new ErroResposta(e.getCodigo(), e.getMessage()));
    }

    // Trata falhas de @Valid nos corpos dos pedidos (ex.: campo obrigatório vazio)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResposta> validacao(MethodArgumentNotValidException e) {
        // Junta todas as mensagens no formato "campo: mensagem; campo2: mensagem2"
        String detalhes = e.getBindingResult().getFieldErrors().stream()
                // Cada erro de campo vira "nomeDoCampo: mensagem"
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                // Separa-os com "; "
                .collect(Collectors.joining("; "));
        // 400 com o código VALIDACAO
        return ResponseEntity.badRequest().body(new ErroResposta("VALIDACAO", detalhes));
    }

    // Trata JSON mal formado no corpo do pedido
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResposta> jsonInvalido(HttpMessageNotReadableException e) {
        // 400 com mensagem simples
        return ResponseEntity.badRequest().body(new ErroResposta("JSON_INVALIDO", "O corpo do pedido não é um JSON válido."));
    }

    // Trata cabeçalhos obrigatórios em falta
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErroResposta> cabecalho(MissingRequestHeaderException e) {
        // 400 indicando qual cabeçalho falta
        return ResponseEntity.badRequest().body(new ErroResposta("CABECALHO_EM_FALTA", "Falta o cabeçalho " + e.getHeaderName()));
    }

    // Rede de segurança: qualquer outro erro inesperado
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResposta> inesperado(Exception e) {
        // Regista tudo, porque é um erro que não previmos
        log.error("Erro inesperado", e);
        // 500 sem detalhes técnicos (não expomos o erro interno ao cliente)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErroResposta("ERRO_INTERNO", "Ocorreu um erro inesperado no módulo de IA."));
    }
}
