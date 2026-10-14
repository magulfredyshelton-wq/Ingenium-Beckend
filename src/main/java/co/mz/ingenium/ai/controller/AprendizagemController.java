package co.mz.ingenium.ai.controller;

import jakarta.validation.Valid;
import co.mz.ingenium.ai.core.AiConstants;
import co.mz.ingenium.ai.core.SseEventos;
import co.mz.ingenium.ai.dto.AprendizagemDtos.*;
import co.mz.ingenium.ai.seguranca.AutenticacaoFilter;
import co.mz.ingenium.ai.seguranca.UtilizadorAutenticado;
import co.mz.ingenium.ai.service.LearningAssistantService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * Necessidade: expor o assistente de aprendizagem.
 * Funções: explicar (SSE), gerar plano de estudo e gerar quiz.
 */
@RestController
@RequestMapping("/api/ai/aprendizagem")
public class AprendizagemController {

    private static final Logger log = LoggerFactory.getLogger(AprendizagemController.class);
    private final LearningAssistantService servico;

    public AprendizagemController(LearningAssistantService servico) {
        this.servico = servico;
    }

    // POST /api/ai/aprendizagem/explicar (streaming)
    @PostMapping(value = "/explicar", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Map<String, String>>> explicar(
            @RequestAttribute(AutenticacaoFilter.ATRIBUTO) UtilizadorAutenticado eu,
            @RequestBody ExplicarPedido pedido) {
        // Validação manual (erros em formato SSE)
        if (pedido == null || pedido.pergunta() == null || pedido.pergunta().isBlank()) {
            return SseEventos.erro("Escreve a tua dúvida.");
        }
        if (pedido.pergunta().length() > 2000) {
            return SseEventos.erro("A dúvida é demasiado longa (máx. 2000 caracteres).");
        }
        try {
            return servico.explicarEmTempoReal(eu.id(), pedido);
        } catch (Exception e) {
            log.error("Falha a preparar explicação", e);
            return SseEventos.erro(AiConstants.ERRO_IA);
        }
    }

    // POST /api/ai/aprendizagem/plano
    @PostMapping("/plano")
    public PlanoEstudo plano(@RequestAttribute(AutenticacaoFilter.ATRIBUTO) UtilizadorAutenticado eu,
                             @Valid @RequestBody PlanoPedido pedido) {
        return servico.gerarPlano(eu.id(), pedido);
    }

    // POST /api/ai/aprendizagem/quiz
    @PostMapping("/quiz")
    public Quiz quiz(@RequestAttribute(AutenticacaoFilter.ATRIBUTO) UtilizadorAutenticado eu,
                     @Valid @RequestBody QuizPedido pedido) {
        return servico.gerarQuiz(eu.id(), pedido);
    }
}
