package co.mz.ingenium.ai.core;

/**
 * Necessidade: cada chamada à IA precisa de levar "quem pediu", "que serviço" e "quando começou",
 * para o AiPromptExecutor registar a duração e o resultado no ai_pedido_log.
 * Funções: transportar esse contexto e calcular a duração da chamada.
 */
public record AiRequestContext(String utilizadorId, String servico, long inicioNanos) {

    /** Cria o contexto no momento em que a chamada começa. */
    public static AiRequestContext iniciar(String utilizadorId, String servico) {
        // System.nanoTime() é o relógio mais preciso para medir durações
        return new AiRequestContext(utilizadorId, servico, System.nanoTime());
    }

    /** Milissegundos desde o início. */
    public long duracaoMs() {
        // Converte nanossegundos em milissegundos
        return (System.nanoTime() - inicioNanos) / 1_000_000;
    }
}
