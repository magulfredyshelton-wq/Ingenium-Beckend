package co.mz.ingenium.ai.exception;

import org.springframework.http.HttpStatus;

/**
 * Necessidade: o Gemini pode falhar (sem internet, chave errada, limite de pedidos).
 * Funções: representar o erro HTTP 503 (Service Unavailable), guardando a causa original.
 */
public class ServicoIaIndisponivelException extends IngeniumException {

    public ServicoIaIndisponivelException(Throwable causa) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "IA_INDISPONIVEL",
                "O Ingenium está temporariamente indisponível. Tenta novamente daqui a pouco.", causa);
    }
}
