package co.mz.ingenium.ai.exception;

import org.springframework.http.HttpStatus;

/**
 * Necessidade: todos os erros "previstos" do módulo precisam de levar consigo o código HTTP
 * e um código curto que o frontend consiga interpretar.
 * Funções: classe-mãe de todas as exceções do Ingenium (estado HTTP + código + mensagem).
 */
// RuntimeException: não obriga a declarar "throws" em todos os métodos
public class IngeniumException extends RuntimeException {

    // Estado HTTP a devolver (400, 404, 502, ...)
    private final HttpStatus estado;
    // Código curto e estável para o frontend (ex.: "PEDIDO_INVALIDO")
    private final String codigo;

    public IngeniumException(HttpStatus estado, String codigo, String mensagem) {
        // A mensagem legível fica guardada na RuntimeException
        super(mensagem);
        this.estado = estado;
        this.codigo = codigo;
    }

    public IngeniumException(HttpStatus estado, String codigo, String mensagem, Throwable causa) {
        // Passa mensagem e causa à classe-mãe
        super(mensagem, causa);
        this.estado = estado;
        this.codigo = codigo;
    }

    public HttpStatus getEstado() {
        return estado;
    }

    public String getCodigo() {
        return codigo;
    }
}
