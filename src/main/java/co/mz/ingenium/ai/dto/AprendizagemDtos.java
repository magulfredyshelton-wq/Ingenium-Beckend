package co.mz.ingenium.ai.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Necessidade: o assistente de aprendizagem explica matérias (streaming), cria planos de estudo
 * e quizzes. Os dois últimos são estruturados para o frontend os mostrar bem.
 * Funções: definir pedidos e formatos (validados) de plano de estudo e quiz.
 */
public final class AprendizagemDtos {

    private AprendizagemDtos() {
    }

    /** Pergunta de estudo (resposta em streaming). */
    public record ExplicarPedido(
            @NotBlank(message = "Escreve a tua dúvida.") @Size(max = 2000) String pergunta,
            // Tema opcional (ex.: "Bases de dados")
            String tema,
            // INICIANTE, INTERMEDIO ou AVANCADO
            String nivel) {
    }

    /** Pedido de plano de estudo. */
    public record PlanoPedido(
            @NotBlank(message = "Indica o objetivo.") @Size(max = 300) String objetivo,
            @Min(1) @Max(24) Integer semanas,
            @Min(1) @Max(40) Integer horasPorSemana) {
    }

    /** Plano de estudo devolvido pelo Gemini. */
    public record PlanoEstudo(
            @NotBlank String titulo,
            String objetivo,
            @NotEmpty @Valid List<SemanaPlano> semanas,
            List<String> recursosSugeridos) {
    }

    /** Uma semana do plano. */
    public record SemanaPlano(
            @Min(1) int semana,
            @NotBlank String foco,
            @NotEmpty List<String> tarefas,
            String resultadoEsperado) {
    }

    /** Pedido de quiz. */
    public record QuizPedido(
            @NotBlank(message = "Indica o tema.") @Size(max = 200) String tema,
            @Min(3) @Max(10) Integer numeroPerguntas,
            String nivel) {
    }

    /** Quiz devolvido pelo Gemini. */
    public record Quiz(
            @NotBlank String tema,
            @NotEmpty @Valid List<PerguntaQuiz> perguntas) {
    }

    /** Uma pergunta de escolha múltipla. */
    public record PerguntaQuiz(
            @NotBlank String enunciado,
            // Exatamente 4 opções
            @NotNull @Size(min = 4, max = 4) List<String> opcoes,
            // Posição da opção certa (0 a 3)
            @Min(0) @Max(3) int indiceCorreto,
            String explicacao) {
    }
}
