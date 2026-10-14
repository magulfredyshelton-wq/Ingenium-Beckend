package co.mz.ingenium.ai.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Necessidade: descrever o resultado das recomendações de vagas e de parceiros, com os
 * números que as justificam (transparência para o estudante).
 * Funções: agrupar os records de recomendação e o formato das explicações geradas pela IA.
 */
public final class RecomendacaoDtos {

    private RecomendacaoDtos() {
    }

    /** Uma vaga recomendada e o porquê. */
    public record VagaRecomendada(
            String vagaId,
            String titulo,
            // Pontuação final 0..1 (similaridade + cobertura de competências)
            double pontuacao,
            // Similaridade semântica dos embeddings 0..1
            double similaridade,
            // Fração das competências da vaga que o estudante já tem 0..1
            double coberturaCompetencias,
            List<String> competenciasEmComum,
            List<String> competenciasEmFalta,
            // Texto curto gerado pelo Ingenium (pode ser null)
            String explicacao) {
    }

    /** Um colega recomendado para equipa/parceria. */
    public record ParceiroRecomendado(
            String utilizadorId,
            String nome,
            double pontuacao,
            double similaridade,
            // O que ele tem e eu não tenho
            List<String> competenciasComplementares,
            // O que temos os dois
            List<String> competenciasEmComum,
            String explicacao) {
    }

    /** Formato JSON que pedimos ao Gemini para as explicações. */
    public record Explicacoes(List<Explicacao> itens) {
    }

    /** Uma explicação ligada ao id da vaga/colega. */
    public record Explicacao(String id, String texto) {
    }

    /** Item do histórico de recomendações de um estudante. */
    public record HistoricoItem(String tipo, String alvoId, double pontuacao, String explicacao,
                                LocalDateTime criadaEm) {
    }
}
