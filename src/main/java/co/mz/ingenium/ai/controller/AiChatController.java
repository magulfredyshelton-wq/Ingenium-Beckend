package co.mz.ingenium.ai.controller;

import co.mz.ingenium.ai.config.IngeniumProperties;
import co.mz.ingenium.ai.core.AiConstants;
import co.mz.ingenium.ai.core.SseEventos;
import co.mz.ingenium.ai.dto.ChatDtos.*;
import co.mz.ingenium.ai.exception.IngeniumException;
import co.mz.ingenium.ai.seguranca.AutenticacaoFilter;
import co.mz.ingenium.ai.seguranca.UtilizadorAutenticado;
import co.mz.ingenium.ai.service.CareerChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * Necessidade: expor o chat do Ingenium ao frontend web e à app mobile.
 * Funções: endpoints REST das conversas e o endpoint SSE de streaming da resposta.
 */
@RestController
@RequestMapping("/api/ai/chat")
public class AiChatController {

    private static final Logger log = LoggerFactory.getLogger(AiChatController.class);

    private final CareerChatService chat;
    // Tamanho máximo de uma mensagem
    private final int maxCaracteres;

    public AiChatController(CareerChatService chat, IngeniumProperties propriedades) {
        this.chat = chat;
        this.maxCaracteres = propriedades.chat().maxCaracteres();
    }

    // POST /api/ai/chat/conversas -> cria conversa com saudação
    @PostMapping("/conversas")
    public ConversaCriada criar(@RequestAttribute(AutenticacaoFilter.ATRIBUTO) UtilizadorAutenticado eu) {
        return chat.novaConversa(eu.exigirId(), eu.nomeOuPadrao());
    }

    // GET /api/ai/chat/conversas -> lista
    @GetMapping("/conversas")
    public List<ConversaResumo> listar(@RequestAttribute(AutenticacaoFilter.ATRIBUTO) UtilizadorAutenticado eu) {
        return chat.listar(eu.exigirId());
    }

    // GET /api/ai/chat/conversas/{id}/mensagens -> histórico
    @GetMapping("/conversas/{id}/mensagens")
    public List<MensagemDto> mensagens(@RequestAttribute(AutenticacaoFilter.ATRIBUTO) UtilizadorAutenticado eu,
                                       @PathVariable Long id) {
        return chat.historico(eu.exigirId(), id);
    }

    // DELETE /api/ai/chat/conversas/{id} -> 204
    @DeleteMapping("/conversas/{id}")
    public ResponseEntity<Void> apagar(@RequestAttribute(AutenticacaoFilter.ATRIBUTO) UtilizadorAutenticado eu,
                                       @PathVariable Long id) {
        chat.apagar(eu.exigirId(), id);
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /api/ai/chat/conversas/{id}/stream (Accept: text/event-stream)
     * Devolve um Flux: o Spring envia cada evento ao cliente assim que é produzido.
     */
    @PostMapping(value = "/conversas/{id}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Map<String, String>>> stream(
            @RequestAttribute(AutenticacaoFilter.ATRIBUTO) UtilizadorAutenticado eu,
            @PathVariable Long id,
            @RequestBody EnviarMensagemPedido pedido) {
        // Pergunta sem espaços nas pontas
        String pergunta = pedido == null || pedido.mensagem() == null ? "" : pedido.mensagem().strip();
        // Validações feitas à mão: o erro tem de sair em formato SSE
        if (pergunta.isEmpty()) {
            return SseEventos.erro("Escreve uma mensagem.");
        }
        if (pergunta.length() > maxCaracteres) {
            return SseEventos.erro("A mensagem é demasiado longa (máx. " + maxCaracteres + " caracteres).");
        }
        try {
            // Erros DURANTE o streaming são tratados no serviço (onErrorResume)
            return chat.responderEmTempoReal(eu.exigirId(), eu.nomeOuPadrao(), id, pergunta);
        } catch (IngeniumException e) {
            return SseEventos.erro(e.getMessage());
        } catch (Exception e) {
            // Falha ANTES do streaming (ex.: pgvector indisponível)
            log.error("Falha a preparar a resposta da conversa {}", id, e);
            return SseEventos.erro(AiConstants.ERRO_IA);
        }
    }
}
