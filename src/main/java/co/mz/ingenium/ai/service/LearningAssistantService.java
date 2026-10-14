package co.mz.ingenium.ai.service;

import co.mz.ingenium.ai.core.AiPromptExecutor;
import co.mz.ingenium.ai.core.AiRequestContext;
import co.mz.ingenium.ai.core.IngeniumPersona;
import co.mz.ingenium.ai.dto.AprendizagemDtos.*;
import co.mz.ingenium.ai.model.TipoEntidade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

import static co.mz.ingenium.ai.core.AiConstants.*;
import static co.mz.ingenium.ai.core.SseEventos.evento;

/**
 * Necessidade: o Ingenium também é um "explicador": tira dúvidas de matérias ao nível do
 * estudante, cria planos de estudo e quizzes para praticar.
 * Funções: explicar em streaming (com RAG na base de conhecimento), gerar plano de estudo
 * estruturado e gerar quiz de escolha múltipla validado.
 */
@Service
public class LearningAssistantService {

    private static final Logger log = LoggerFactory.getLogger(LearningAssistantService.class);

    private final AiPromptExecutor executor;
    private final PesquisaSemanticaService pesquisa;
    private final IngeniumPersona persona;

    public LearningAssistantService(AiPromptExecutor executor, PesquisaSemanticaService pesquisa, IngeniumPersona persona) {
        this.executor = executor;
        this.pesquisa = pesquisa;
        this.persona = persona;
    }

    /** Explica uma dúvida, com a resposta a aparecer em tempo real. */
    public Flux<ServerSentEvent<Map<String, String>>> explicarEmTempoReal(String utilizadorId, ExplicarPedido p) {
        // RAG: materiais da base de conhecimento relacionados com a dúvida
        String consulta = (p.tema() == null ? "" : p.tema() + ": ") + p.pergunta();
        List<Document> materiais = pesquisa.pesquisarPorTipos(consulta, TipoEntidade.CONHECIMENTO);
        // Instruções pedagógicas
        String sistema = persona.identidade() + """

                PAPEL: és um explicador paciente. Nível do estudante: %s.
                MÉTODO: 1) explica a ideia principal em linguagem simples; 2) dá um exemplo do dia a dia \
                em Moçambique; 3) se for exercício, resolve passo a passo; 4) termina com uma pergunta \
                curta para o estudante verificar se percebeu.
                Usa os MATERIAIS abaixo quando forem úteis; caso contrário usa o teu conhecimento geral.

                MATERIAIS:
                %s
                """.formatted(nivel(p.nivel()), PesquisaSemanticaService.formatarContexto(materiais));
        // Prompt
        Prompt prompt = executor.montar(sistema, p.pergunta());
        // Streaming: inicio -> tokens -> fim
        return Flux.just(evento(EVENTO_INICIO, Map.of("servico", SERVICO_APRENDIZAGEM)))
                .concatWith(executor.executarStream(AiRequestContext.iniciar(utilizadorId, SERVICO_APRENDIZAGEM), prompt)
                        .map(t -> evento(EVENTO_TOKEN, Map.of("t", t))))
                .concatWith(Flux.just(evento(EVENTO_FIM, Map.of())))
                .onErrorResume(e -> {
                    log.error("Falha no assistente de aprendizagem", e);
                    return Flux.just(evento(EVENTO_ERRO, Map.of("erro", ERRO_IA)));
                });
    }

    /** Gera um plano de estudo semana a semana. */
    public PlanoEstudo gerarPlano(String utilizadorId, PlanoPedido p) {
        // Valores por omissão
        int semanas = p.semanas() == null ? 4 : p.semanas();
        int horas = p.horasPorSemana() == null ? 5 : p.horasPorSemana();
        // Instruções
        String sistema = persona.identidade() + """

                TAREFA: criar um plano de estudo realista para um estudante do secundário/técnico.
                Exatamente %d semanas, cerca de %d horas por semana. Cada semana: foco, 3 a 5 tarefas \
                práticas e o resultado esperado. Em "recursosSugeridos" indica tipos de recursos \
                gratuitos (ex.: documentação oficial, vídeos, exercícios) sem inventar links.
                """.formatted(semanas, horas);
        // Chamada estruturada
        PlanoEstudo plano = executor.executarEstruturado(AiRequestContext.iniciar(utilizadorId, SERVICO_APRENDIZAGEM),
                sistema, "Objetivo: " + p.objetivo(), PlanoEstudo.class);
        // Garante que não vem com mais semanas do que o pedido
        List<SemanaPlano> lista = plano.semanas().stream().limit(semanas).toList();
        return new PlanoEstudo(plano.titulo(), p.objetivo(), lista,
                plano.recursosSugeridos() == null ? List.of() : plano.recursosSugeridos());
    }

    /** Gera um quiz de escolha múltipla (4 opções, 1 certa). */
    public Quiz gerarQuiz(String utilizadorId, QuizPedido p) {
        // Número de perguntas (padrão 5)
        int n = p.numeroPerguntas() == null ? 5 : p.numeroPerguntas();
        // Instruções
        String sistema = persona.identidade() + """

                TAREFA: criar um quiz de escolha múltipla com exatamente %d perguntas, nível %s.
                Cada pergunta tem exatamente 4 opções, só uma correta; "indiceCorreto" é a posição \
                da correta (0 a 3). "explicacao" diz em 1 frase porque está certa. Varia a posição \
                da opção correta.
                """.formatted(n, nivel(p.nivel()));
        // Chamada estruturada (as regras @Size(4) e @Min/@Max(0..3) são verificadas no validador)
        Quiz quiz = executor.executarEstruturado(AiRequestContext.iniciar(utilizadorId, SERVICO_APRENDIZAGEM),
                sistema, "Tema: " + p.tema(), Quiz.class);
        // Corta ao número pedido
        return new Quiz(quiz.tema(), quiz.perguntas().stream().limit(n).toList());
    }

    // Converte o código do nível em descrição
    private static String nivel(String nivel) {
        if (nivel == null) {
            return "iniciante";
        }
        return switch (nivel.strip().toUpperCase()) {
            case "AVANCADO", "AVANÇADO" -> "avançado";
            case "INTERMEDIO", "INTERMÉDIO", "INTERMEDIARIO" -> "intermédio";
            default -> "iniciante";
        };
    }
}
