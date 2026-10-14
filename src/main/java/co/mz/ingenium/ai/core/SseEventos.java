package co.mz.ingenium.ai.core;

import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * Necessidade: o streaming usa Server-Sent Events (SSE). Todos os endpoints de streaming
 * têm de criar eventos com o mesmo formato: "event: token / data: {"t":"..."}".
 * Funções: criar um evento SSE e criar uma resposta de erro em formato SSE.
 */
public final class SseEventos {

    private SseEventos() {
    }

    /** Cria um evento com nome e dados; o Spring converte o Map em JSON sozinho. */
    public static ServerSentEvent<Map<String, String>> evento(String nome, Map<String, String> dados) {
        return ServerSentEvent.<Map<String, String>>builder()
                // Nome do evento (inicio, token, fim, erro)
                .event(nome)
                // Conteúdo do evento
                .data(dados)
                // Constrói o evento
                .build();
    }

    /** Corrente com um único evento de erro (o frontend mostra a mensagem). */
    public static Flux<ServerSentEvent<Map<String, String>>> erro(String mensagem) {
        return Flux.just(evento(AiConstants.EVENTO_ERRO, Map.of("erro", mensagem)));
    }
}
