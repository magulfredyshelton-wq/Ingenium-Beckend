package co.mz.ingenium.ai.exception;

import org.springframework.http.HttpStatus;

/**
 * Necessidade: distinguir erros causados por dados errados enviados pelo cliente.
 * Funções: representar o erro HTTP 400 (Bad Request).
 */
public class PedidoInvalidoException extends IngeniumException {

    public PedidoInvalidoException(String mensagem) {
        super(HttpStatus.BAD_REQUEST, "PEDIDO_INVALIDO", mensagem);
    }
}
