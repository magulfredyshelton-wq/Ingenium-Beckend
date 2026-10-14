package co.mz.ingenium.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;

/**
 * Necessidade: reunir num só sítio, com tipos Java, todas as propriedades "ingenium.ai.*"
 * do application.properties. Evita espalhar @Value("...") com texto pelo código.
 * Funções: expor o nome da IA, as chaves de segurança, as origens CORS, os parâmetros
 * do RAG, do chat e a pasta da taxonomia ESCO.
 */
// Liga todas as propriedades que começam por "ingenium.ai" a este record
@ConfigurationProperties(prefix = "ingenium.ai")
public record IngeniumProperties(
        // ingenium.ai.nome -> nome com que a IA se apresenta ("Ingenium")
        @DefaultValue("Ingenium") String nome,

        // ingenium.ai.rag.*
        @DefaultValue Rag rag,
        // ingenium.ai.chat.*
        @DefaultValue Chat chat) {


    /** Parâmetros da recuperação (R do RAG). */
    public record Rag(
            // Quantos pedaços de texto recuperar da base vetorial
            @DefaultValue("6") int topK,
            // Similaridade mínima (0..1) para um pedaço ser considerado relevante
            @DefaultValue("0.55") double limiarSimilaridade) {
    }

    /** Parâmetros do chat. */
    public record Chat(
            // Quantas mensagens anteriores entram na memória da conversa
            @DefaultValue("10") int memoriaMensagens,
            // Tamanho máximo de uma mensagem do estudante
            @DefaultValue("4000") int maxCaracteres) {
    }
}
