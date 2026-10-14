package co.mz.ingenium.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Necessidade: definir exatamente que dados entram e saem dos endpoints do chat, sem expor as
 * entidades JPA diretamente.
 * Funções: agrupar os records (DTOs) do chat com o Ingenium.
 */
public final class ChatDtos {

    private ChatDtos() {
    }

    /** Uma conversa na lista lateral. */
    public record ConversaResumo(Long id, String titulo, LocalDateTime atualizadaEm) {
    }

    /** Uma mensagem do histórico. */
    public record MensagemDto(Long id, String papel, String conteudo, LocalDateTime criadaEm) {
    }

    /** Resposta ao criar conversa: a conversa + a saudação do Ingenium. */
    public record ConversaCriada(ConversaResumo conversa, MensagemDto saudacao) {
    }

    /** Corpo do POST com a pergunta do estudante. */
    public record EnviarMensagemPedido(
            // Não pode vir vazia
            @NotBlank(message = "Escreve uma mensagem.")
            // Limite de tamanho (protege a quota do Gemini)
            @Size(max = 4000, message = "A mensagem não pode ter mais de 4000 caracteres.")
            String mensagem) {
    }
}
