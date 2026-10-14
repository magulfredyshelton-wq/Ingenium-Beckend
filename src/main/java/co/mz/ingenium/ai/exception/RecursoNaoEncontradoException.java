package co.mz.ingenium.ai.exception;

import org.springframework.http.HttpStatus;

/**
 * Necessidade: sinalizar que algo pedido não existe (ou não pertence ao utilizador).
 * Funções: representar o erro HTTP 404 (Not Found).
 */
public class RecursoNaoEncontradoException extends IngeniumException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(HttpStatus.NOT_FOUND, "NAO_ENCONTRADO", mensagem);
    }
}
