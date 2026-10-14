package co.mz.ingenium.ai.exception;

import org.springframework.http.HttpStatus;

/**
 * Necessidade: impedir que um estudante use endpoints reservados ao backend/administração.
 * Funções: representar o erro HTTP 403 (Forbidden).
 */
public class AcessoNegadoException extends IngeniumException {

    public AcessoNegadoException(String mensagem) {
        super(HttpStatus.FORBIDDEN, "ACESSO_NEGADO", mensagem);
    }
}
