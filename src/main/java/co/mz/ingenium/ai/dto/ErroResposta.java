package co.mz.ingenium.ai.dto;

/**
 * Necessidade: o frontend precisa de receber os erros sempre no mesmo formato JSON.
 * Funções: transportar um código curto (para o programa) e uma mensagem (para a pessoa).
 */
public record ErroResposta(String codigo, String mensagem) {
}
