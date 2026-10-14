package co.mz.ingenium.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * Necessidade: o módulo de feed precisa de saber se uma publicação é adequada a estudantes,
 * de que tipo é e que hashtags (normalizadas pela taxonomia) deve ter.
 * Funções: definir o pedido, o formato devolvido pelo Gemini e a resposta final.
 */
public final class PostDtos {

    private PostDtos() {
    }

    /** Texto da publicação a analisar. */
    public record AnalisarPostPedido(
            @NotBlank(message = "O texto é obrigatório.")
            @Size(max = 5000, message = "Máximo de 5000 caracteres.")
            String texto) {
    }

    /** Formato JSON que o Gemini tem de devolver. */
    public record AnaliseIa(
            boolean adequado,
            String motivo,
            @NotBlank String categoria,
            @NotNull List<String> tags,
            String tom,
            String sugestaoMelhoria) {
    }

    /** Tag da base de dados sugerida para o post. */
    public record TagSugerida(UUID id, String nome, String slug, String categoria) {
    }

    /** Resposta final para o feed. */
    public record AnalisePost(boolean adequado, String motivo, String categoria, String tom,
                              String sugestaoMelhoria, List<TagSugerida> tags) {
    }
}
