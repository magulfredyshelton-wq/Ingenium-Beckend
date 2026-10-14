package co.mz.ingenium.ai.service;

import co.mz.ingenium.ai.config.IngeniumProperties;
import co.mz.ingenium.ai.core.AiPromptExecutor;
import co.mz.ingenium.ai.core.AiRequestContext;
import co.mz.ingenium.ai.core.IngeniumPersona;
import co.mz.ingenium.ai.dto.ChatDtos.ConversaCriada;
import co.mz.ingenium.ai.dto.ChatDtos.ConversaResumo;
import co.mz.ingenium.ai.dto.ChatDtos.MensagemDto;
import co.mz.ingenium.ai.exception.RecursoNaoEncontradoException;
import co.mz.ingenium.ai.model.*;
import co.mz.ingenium.ai.model.*;
import co.mz.ingenium.ai.repository.ConversaRepository;
import co.mz.ingenium.ai.repository.EntidadeIndexadaRepository;
import co.mz.ingenium.ai.repository.MensagemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static co.mz.ingenium.ai.core.AiConstants.*;
import static co.mz.ingenium.ai.core.SseEventos.evento;

/**
 * Necessidade: é o serviço principal que o estudante vê: o chat com o Ingenium sobre carreira,
 * estudos e a plataforma. Tem de usar RAG (conhecimento + vagas + perfil do estudante), lembrar
 * a conversa e mostrar a resposta a ser escrita (streaming).
 * Funções: criar/listar/apagar conversas, devolver o histórico e responder em tempo real
 * (Retrieval -> Augmentation -> Generation em streaming -> gravação no fim).
 */
@Service
public class CareerChatService {

    private static final Logger log = LoggerFactory.getLogger(CareerChatService.class);
    // Formato da data mostrada à IA
    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ConversaRepository conversas;
    private final MensagemRepository mensagens;
    private final EntidadeIndexadaRepository entidades;
    private final PesquisaSemanticaService pesquisa;
    private final AiPromptExecutor executor;
    private final IngeniumPersona persona;
    private final TransactionTemplate transacao;
    // Quantas mensagens anteriores entram na memória
    private final int memoria;

    public CareerChatService(ConversaRepository conversas, MensagemRepository mensagens,
                             EntidadeIndexadaRepository entidades, PesquisaSemanticaService pesquisa,
                             AiPromptExecutor executor, IngeniumPersona persona,
                             TransactionTemplate transacao, IngeniumProperties propriedades) {
        this.conversas = conversas;
        this.mensagens = mensagens;
        this.entidades = entidades;
        this.pesquisa = pesquisa;
        this.executor = executor;
        this.persona = persona;
        this.transacao = transacao;
        this.memoria = propriedades.chat().memoriaMensagens();
    }

    // =====================================================================
    //  CONVERSAS
    // =====================================================================

    /** Cria uma conversa e grava a saudação do Ingenium como 1.ª mensagem. */
    @Transactional
    public ConversaCriada novaConversa(String utilizadorId, String nomeToken) {
        // Nome real (do perfil indexado) ou o nome que veio no token
        String nome = nomeDoEstudante(utilizadorId, nomeToken);
        // Grava a conversa
        Conversa conversa = conversas.save(new Conversa(utilizadorId, TITULO_INICIAL));
        // Grava a saudação
        Mensagem saudacao = mensagens.save(new Mensagem(conversa, PapelMensagem.ASSISTENTE, persona.saudacao(nome)));
        // Devolve ambas ao frontend
        return new ConversaCriada(resumo(conversa), dto(saudacao));
    }

    /** Conversas do estudante, mais recentes primeiro. */
    @Transactional(readOnly = true)
    public List<ConversaResumo> listar(String utilizadorId) {
        return conversas.findByUtilizadorIdOrderByAtualizadaEmDesc(utilizadorId).stream().map(this::resumo).toList();
    }

    /** Histórico completo de uma conversa (só se for do estudante). */
    @Transactional(readOnly = true)
    public List<MensagemDto> historico(String utilizadorId, Long conversaId) {
        // Garante a posse (lança 404 se não for dele)
        obter(utilizadorId, conversaId);
        // Mensagens por ordem
        return mensagens.findByConversaIdOrderByIdAsc(conversaId).stream().map(this::dto).toList();
    }

    /** Apaga uma conversa (as mensagens caem em cascata na BD). */
    @Transactional
    public void apagar(String utilizadorId, Long conversaId) {
        conversas.delete(obter(utilizadorId, conversaId));
    }

    // =====================================================================
    //  RESPONDER EM TEMPO REAL (RAG + STREAMING)
    // =====================================================================

    public Flux<ServerSentEvent<Map<String, String>>> responderEmTempoReal(
            String utilizadorId, String nomeToken, Long conversaId, String pergunta) {
        // 1) A conversa tem de ser deste estudante
        obter(utilizadorId, conversaId);
        // 2) Monta o prompt completo (RAG + perfil + memória + pergunta)
        Prompt prompt = montarPrompt(utilizadorId, nomeToken, conversaId, pergunta);
        // Contexto para o registo da chamada
        AiRequestContext ctx = AiRequestContext.iniciar(utilizadorId, SERVICO_CHAT);
        // Acumula o texto completo para gravar no fim
        StringBuilder completa = new StringBuilder();

        // Evento inicial: o frontend sabe que a resposta começou
        Flux<ServerSentEvent<Map<String, String>>> inicio =
                Flux.just(evento(EVENTO_INICIO, Map.of("conversaId", String.valueOf(conversaId))));

        // 3) Geração em streaming: cada bocado do Gemini vira um evento "token"
        Flux<ServerSentEvent<Map<String, String>>> bocados = executor.executarStream(ctx, prompt)
                // Guarda cada bocado
                .doOnNext(completa::append)
                // Converte em evento SSE
                .map(t -> evento(EVENTO_TOKEN, Map.of("t", t)));

        // 4) Depois do último bocado: gravar na BD e enviar "fim"
        Flux<ServerSentEvent<Map<String, String>>> fim = Flux.defer(() -> {
            // Lista de eventos finais
            List<ServerSentEvent<Map<String, String>>> eventos = new ArrayList<>();
            // Texto completo
            String resposta = completa.toString();
            // Resposta vazia: envia a mensagem padrão
            if (resposta.isBlank()) {
                resposta = RESPOSTA_VAZIA;
                eventos.add(evento(EVENTO_TOKEN, Map.of("t", resposta)));
            }
            // Grava pergunta e resposta juntas
            String titulo = guardarTroca(conversaId, pergunta, resposta);
            // Evento final com o título (pode ter mudado)
            eventos.add(evento(EVENTO_FIM, Map.of("titulo", titulo)));
            return Flux.fromIterable(eventos);
        }).subscribeOn(Schedulers.boundedElastic());

        // Junta tudo por ordem; qualquer erro vira um evento "erro"
        return inicio.concatWith(bocados).concatWith(fim)
                .onErrorResume(e -> {
                    log.error("Falha no chat da conversa {}", conversaId, e);
                    return Flux.just(evento(EVENTO_ERRO, Map.of("erro", ERRO_IA)));
                });
    }

    // =====================================================================
    //  INTERNOS
    // =====================================================================

    // Constrói system + histórico + pergunta
    private Prompt montarPrompt(String utilizadorId, String nomeToken, Long conversaId, String pergunta) {
        // R do RAG: pedaços de conhecimento e vagas parecidos com a pergunta
        List<Document> relevantes = pesquisa.pesquisarPorTipos(pergunta, TipoEntidade.CONHECIMENTO, TipoEntidade.VAGA);
        log.debug("Conversa {}: {} pedaços relevantes", conversaId, relevantes.size());
        // Perfil do estudante (sempre incluído, não depende da pesquisa)
        String perfil = entidades.findByTipoAndReferenciaId(TipoEntidade.PERFIL, utilizadorId)
                .map(EntidadeIndexada::getTexto)
                .orElse("(o estudante ainda não completou o perfil no Ingenium)");
        // Memória: últimas N mensagens (vêm da mais recente para a mais antiga)
        List<Mensagem> recentes = new ArrayList<>(
                mensagens.findByConversaIdOrderByIdDesc(conversaId, PageRequest.of(0, memoria)));
        // Põe por ordem cronológica
        Collections.reverse(recentes);
        // A do RAG: lista de mensagens para o Gemini
        List<Message> lista = new ArrayList<>();
        // Instruções de sistema com o contexto recuperado
        lista.add(new SystemMessage(instrucoes(nomeDoEstudante(utilizadorId, nomeToken), perfil, relevantes)));
        // Histórico
        for (Mensagem m : recentes) {
            lista.add(m.getPapel() == PapelMensagem.UTILIZADOR
                    ? new UserMessage(m.getConteudo())
                    : new AssistantMessage(m.getConteudo()));
        }
        // Pergunta atual
        lista.add(new UserMessage(pergunta));
        // Prompt final
        return new Prompt(lista);
    }

    // Texto do system prompt do chat
    private String instrucoes(String nome, String perfil, List<Document> relevantes) {
        // Data e hora de Maputo
        String agora = LocalDateTime.now(FUSO).format(FORMATO_DATA);
        return persona.identidade() + """

                CONTEXTO DA CONVERSA:
                - Estás a falar com %s. Trata-o pelo primeiro nome (%s).
                - Data e hora atuais: %s (Maputo).

                PERFIL DO ESTUDANTE:
                %s

                INFORMAÇÃO DA PLATAFORMA (base de conhecimento e vagas abertas):
                %s

                COMO RESPONDER:
                - Para perguntas sobre vagas, a plataforma ou orientação de carreira, usa primeiro a \
                INFORMAÇÃO DA PLATAFORMA. Só menciona vagas que aparecem lá.
                - Personaliza os conselhos com o PERFIL DO ESTUDANTE.
                - Para perguntas gerais de estudo, usa o teu conhecimento geral.
                - Não fales em "contexto", "documentos" ou "base de dados": responde com naturalidade.
                - Termina, quando fizer sentido, com um próximo passo concreto.
                """.formatted(nome, persona.primeiroNome(nome), agora, perfil,
                PesquisaSemanticaService.formatarContexto(relevantes));
    }

    // Grava pergunta + resposta numa transação e devolve o título da conversa
    private String guardarTroca(Long conversaId, String pergunta, String resposta) {
        return transacao.execute(status -> {
            // Recarrega a conversa dentro da transação
            Conversa conversa = conversas.findById(conversaId)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Conversa não encontrada."));
            // A 1.ª pergunta passa a ser o título
            if (TITULO_INICIAL.equals(conversa.getTitulo())) {
                conversa.setTitulo(pergunta.length() > 60 ? pergunta.substring(0, 57) + "..." : pergunta);
            }
            // Atualiza a data
            conversa.marcarAtualizada();
            // Grava a conversa e as duas mensagens
            conversas.save(conversa);
            mensagens.save(new Mensagem(conversa, PapelMensagem.UTILIZADOR, pergunta));
            mensagens.save(new Mensagem(conversa, PapelMensagem.ASSISTENTE, resposta));
            return conversa.getTitulo();
        });
    }

    // Conversa do estudante ou 404
    private Conversa obter(String utilizadorId, Long conversaId) {
        return conversas.findByIdAndUtilizadorId(conversaId, utilizadorId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Conversa não encontrada."));
    }

    // Nome do perfil indexado; se não existir, o do token
    private String nomeDoEstudante(String utilizadorId, String nomeToken) {
        return entidades.findByTipoAndReferenciaId(TipoEntidade.PERFIL, utilizadorId)
                .map(EntidadeIndexada::getTitulo)
                .filter(n -> n != null && !n.isBlank())
                .orElse(nomeToken);
    }

    // Entidade -> DTO
    private ConversaResumo resumo(Conversa c) {
        return new ConversaResumo(c.getId(), c.getTitulo(), c.getAtualizadaEm());
    }

    // Entidade -> DTO
    private MensagemDto dto(Mensagem m) {
        return new MensagemDto(m.getId(), m.getPapel().name(), m.getConteudo(), m.getCriadaEm());
    }
}
