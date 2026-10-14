package co.mz.ingenium.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Necessidade: permitir traduzir perfis, vagas e publicações (ex.: vagas em inglês).
 * Funções: definir o pedido e a resposta da tradução.
 */
public final class TraducaoDtos {

    private TraducaoDtos() {
    }

    /** Texto e idioma de destino (código: pt, en, fr, es, sw). */
    public record TraduzirPedido(
            @NotBlank(message = "O texto é obrigatório.") @Size(max = 5000) String texto,
            @NotBlank(message = "Indica o idioma de destino.") String idiomaDestino) {
    }

    /** Texto traduzido. */
    public record TraducaoResposta(String traducao, String idiomaDestino) {
    }
}
