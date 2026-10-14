package co.mz.ingenium.ai.core;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import co.mz.ingenium.ai.exception.RespostaIaInvalidaException;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Necessidade: decisão do documento "Visão da IA": NUNCA confiar cegamente na resposta da LLM.
 * Ela pode vir vazia, gigante, com listas repetidas ou com números fora do intervalo.
 * Funções: validar texto, validar objetos estruturados (anotações @NotBlank, @Min...), limpar
 * listas e limitar valores numéricos.
 */
@Component
public class AiResponseValidator {

    // Limite de segurança para o tamanho de uma resposta de texto
    private static final int MAX_TEXTO = 20_000;

    // Validador do Jakarta Bean Validation (criado pelo Spring Boot)
    private final Validator validador;

    public AiResponseValidator(Validator validador) {
        this.validador = validador;
    }

    /** Garante que o texto existe; tira espaços e corta se for enorme. */
    public String validarTexto(String resposta) {
        // Resposta nula ou só espaços é inválida
        if (resposta == null || resposta.isBlank()) {
            throw new RespostaIaInvalidaException("O Ingenium não conseguiu gerar uma resposta. Tenta novamente.");
        }
        // Remove espaços/linhas vazias nas pontas
        String limpa = resposta.strip();
        // Corta respostas exageradamente longas
        return limpa.length() > MAX_TEXTO ? limpa.substring(0, MAX_TEXTO) : limpa;
    }

    /** Aplica as regras de validação declaradas no record (ex.: @Min(0) @Max(100)). */
    public <T> T validarObjeto(T objeto) {
        // A conversão de JSON pode ter devolvido null
        if (objeto == null) {
            throw new RespostaIaInvalidaException("O Ingenium devolveu uma resposta vazia.");
        }
        // Corre todas as anotações de validação
        Set<ConstraintViolation<T>> violacoes = validador.validate(objeto);
        // Se alguma regra falhou...
        if (!violacoes.isEmpty()) {
            // ...junta as falhas num texto "campo: problema"
            String detalhe = violacoes.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining("; "));
            // ...e rejeita a resposta
            throw new RespostaIaInvalidaException("A resposta do Ingenium veio incompleta (" + detalhe + "). Tenta novamente.");
        }
        return objeto;
    }

    /** Remove nulos, vazios e repetidos; tira espaços; limita a quantidade. */
    public List<String> limparLista(List<String> lista, int maximo) {
        // Lista nula vira lista vazia
        if (lista == null) {
            return List.of();
        }
        // LinkedHashSet elimina repetidos mantendo a ordem
        Set<String> unicos = new LinkedHashSet<>();
        // Percorre os itens
        for (String item : lista) {
            // Ignora nulos e vazios
            if (item != null && !item.isBlank()) {
                // Guarda sem espaços nas pontas
                unicos.add(item.strip());
            }
        }
        return unicos.stream().limit(maximo).toList();
    }

    /** Força um inteiro a ficar entre min e max. */
    public int limitar(int valor, int min, int max) {
        return Math.max(min, Math.min(max, valor));
    }
}
