package co.mz.ingenium.ai.seguranca;

import co.mz.ingenium.ai.exception.AcessoNegadoException;
import co.mz.ingenium.ai.exception.PedidoInvalidoException;

/**
 * Necessidade: depois de confirmar quem fez o pedido, os controllers precisam dessa informação
 * (id, nome, papel) num objeto simples.
 * Funções: guardar a identidade do pedido e oferecer verificações de permissão.
 */
public record UtilizadorAutenticado(String id, String nome, String papel) {

    // Papel de um estudante normal
    public static final String ESTUDANTE = "ESTUDANTE";
    // Papel de uma empresa (utilizador do tipo Company)
    public static final String EMPRESA = "EMPRESA";
    // Papel de administrador da plataforma
    public static final String ADMIN = "ADMIN";
    // Papel usado quando quem chama é o backend principal (com X-API-KEY)
    public static final String SISTEMA = "SISTEMA";

    // true se o pedido veio do backend principal
    public boolean ehSistema() {
        return SISTEMA.equals(papel);
    }

    // true se é administrador
    public boolean ehAdmin() {
        return ADMIN.equalsIgnoreCase(papel);
    }

    /** Garante que existe um id de utilizador (o backend tem de enviar X-User-Id). */
    public String exigirId() {
        // Sem id não sabemos em nome de quem responder
        if (id == null || id.isBlank()) {
            // Erro 400 com instrução clara
            throw new PedidoInvalidoException("Indica o utilizador no cabeçalho X-User-Id.");
        }
        return id;
    }

    /** Garante que o pedido vem do backend ou de um administrador. */
    public void exigirSistemaOuAdmin() {
        // Estudantes não podem indexar vagas nem importar taxonomia
        if (!ehSistema() && !ehAdmin()) {
            // Erro 403
            throw new AcessoNegadoException("Operação reservada ao backend do Ingenium ou a administradores.");
        }
    }

    /** Nome a usar nas saudações; nunca devolve vazio. */
    public String nomeOuPadrao() {
        // Se não veio nome no token, usa "Estudante"
        return (nome == null || nome.isBlank()) ? "Estudante" : nome;
    }
}