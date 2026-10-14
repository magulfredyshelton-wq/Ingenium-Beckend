package co.mz.ingenium.ai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Necessidade: o serviço de melhoria de perfil devolve uma análise estruturada (não um texto
 * solto), para o frontend a mostrar em cartões.
 * Funções: definir o pedido (objetivo opcional) e o formato da análise, com regras de validação
 * que o AiResponseValidator aplica à resposta do Gemini.
 */
public final class PerfilDtos {

    private PerfilDtos() {
    }

    /** Pedido opcional: o objetivo do estudante (ex.: "estágio em redes"). */
    public record PedidoSugestoes(@Size(max = 300) String objetivo) {
    }

    /** Análise do perfil (formato que o Gemini tem de respeitar). */
    public record SugestoesPerfil(
            // Nota de 0 a 100
            @Min(0) @Max(100) int pontuacao,
            // Resumo em 1-2 frases
            @NotBlank String resumo,
            @NotNull List<String> pontosFortes,
            @NotNull List<String> melhorias,
            @NotNull List<String> competenciasSugeridas,
            // Proposta de novo texto "Sobre mim"
            String sobreSugerido) {
    }
}
