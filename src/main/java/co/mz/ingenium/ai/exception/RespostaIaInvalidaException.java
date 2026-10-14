package co.mz.ingenium.ai.exception;

import org.springframework.http.HttpStatus;

/**
 * Necessidade: a LLM pode devolver texto vazio ou JSON fora do formato. Isso não é culpa
 * do cliente nem um erro de programação: é uma resposta inválida do "servidor de cima".
 * Funções: representar o erro HTTP 502 (Bad Gateway) quando a validação da resposta falha.
 */
public class RespostaIaInvalidaException extends IngeniumException {

    public RespostaIaInvalidaException(String mensagem) {
        super(HttpStatus.BAD_GATEWAY, "RESPOSTA_IA_INVALIDA", mensagem);
    }
}
