package co.mz.ingenium.ai.core;

import co.mz.ingenium.ai.exception.IngeniumException;
import co.mz.ingenium.ai.exception.RespostaIaInvalidaException;
import co.mz.ingenium.ai.exception.ServicoIaIndisponivelException;
import co.mz.ingenium.ai.model.PedidoLog;
import co.mz.ingenium.ai.repository.PedidoLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.List;

/**
 * Necessidade: os 7 serviços falam com o Gemini. Sem um ponto central, cada um repetiria o
 * tratamento de erros, a validação e o registo. (Composição em vez de herança: os serviços
 * USAM este executor, não herdam dele.)
 * Funções: executar prompts em 3 modos (texto, JSON estruturado e streaming), converter falhas
 * técnicas em exceções do Ingenium e registar cada chamada em ai_pedido_log.
 */
@Component
public class AiPromptExecutor {

    private static final Logger log = LoggerFactory.getLogger(AiPromptExecutor.class);

    private final ChatClient chatClient;
    private final AiResponseValidator validador;
    private final PedidoLogRepository registos;

    public AiPromptExecutor(ChatClient chatClient, AiResponseValidator validador, PedidoLogRepository registos) {
        this.chatClient = chatClient;
        this.validador = validador;
        this.registos = registos;
    }

    /**
     * Monta um Prompt com instruções de sistema + mensagem do utilizador.
     * Usamos objetos Message (e não texto com {variaveis}) para que chavetas vindas do
     * utilizador ou do JSON não sejam confundidas com variáveis de template.
     */
    public Prompt montar(String sistema, String utilizador) {
        // SystemMessage = regras/persona; UserMessage = o pedido concreto
        return new Prompt(List.of(new SystemMessage(sistema), new UserMessage(utilizador)));
    }

    /** MODO 1: resposta completa em texto. */
    public String executarTexto(AiRequestContext ctx, String sistema, String utilizador) {
        try {
            // call() espera pela resposta completa; content() devolve só o texto
            String resposta = chatClient.prompt(montar(sistema, utilizador)).call().content();
            // Valida (não vazia, tamanho razoável)
            String valida = validador.validarTexto(resposta);
            registar(ctx, true, null);
            return valida;
        } catch (IngeniumException e) {
            // Erro já "traduzido" (ex.: resposta vazia): regista e volta a lançar
            registar(ctx, false, e.getMessage());
            throw e;
        } catch (RuntimeException e) {
            // Erro técnico (rede, chave, limite do Gemini): regista e converte em 503
            registar(ctx, false, e.getMessage());
            throw new ServicoIaIndisponivelException(e);
        }
    }

    /**
     * MODO 2: resposta estruturada. O BeanOutputConverter gera, a partir do record Java,
     * instruções + JSON Schema que o Gemini tem de seguir, e depois converte o JSON no record.
     */
    public <T> T executarEstruturado(AiRequestContext ctx, String sistema, String utilizador, Class<T> tipo) {
        // Conversor ligado à classe pedida (ex.: SugestoesPerfil.class)
        BeanOutputConverter<T> conversor = new BeanOutputConverter<>(tipo);
        // Acrescenta às instruções o formato JSON obrigatório
        String sistemaComFormato = sistema + "\n\n" + conversor.getFormat();
        try {
            // Pede a resposta completa ao Gemini
            String bruto = chatClient.prompt(montar(sistemaComFormato, utilizador)).call().content();
            // Não pode vir vazia
            String texto = validador.validarTexto(bruto);
            // Variável que vai receber o objeto convertido
            T objeto;
            try {
                // Converte o JSON (o conversor remove ```json ... ``` se vier)
                objeto = conversor.convert(texto);
            } catch (RuntimeException e) {
                // JSON inválido = resposta inválida (502)
                throw new RespostaIaInvalidaException("O Ingenium devolveu um formato inesperado. Tenta novamente.");
            }
            // Aplica as regras @NotBlank, @Min, @Size... do record
            validador.validarObjeto(objeto);
            registar(ctx, true, null);
            return objeto;
        } catch (IngeniumException e) {
            registar(ctx, false, e.getMessage());
            throw e;
        } catch (RuntimeException e) {
            registar(ctx, false, e.getMessage());
            throw new ServicoIaIndisponivelException(e);
        }
    }

    /**
     * MODO 3: streaming. Devolve um Flux: cada elemento é um bocado de texto assim que o Gemini
     * o gera. É isto que permite mostrar a resposta "a ser escrita" no ecrã.
     */
    public Flux<String> executarStream(AiRequestContext ctx, Prompt prompt) {
        // defer: a chamada só começa quando alguém subscrever o Flux (o Spring MVC)
        return Flux.defer(() -> chatClient.prompt(prompt).stream().content())
                // Ignora bocados vazios
                .filter(t -> t != null && !t.isEmpty())
                // No fim com sucesso, regista
                .doOnComplete(() -> registar(ctx, true, null))
                // Em caso de erro, regista a falha
                .doOnError(e -> registar(ctx, false, e.getMessage()));
    }

    /** Grava o registo da chamada numa thread à parte (nunca atrasa nem parte a resposta). */
    private void registar(AiRequestContext ctx, boolean sucesso, String erro) {
        // Calcula a duração já, antes de mudar de thread
        long duracao = ctx.duracaoMs();
        // boundedElastic = threads próprias para trabalho bloqueante (base de dados)
        Schedulers.boundedElastic().schedule(() -> {
            try {
                // Insere a linha em ai_pedido_log
                registos.save(new PedidoLog(ctx.utilizadorId(), ctx.servico(), duracao, sucesso, erro));
            } catch (RuntimeException e) {
                // Só avisa no log
                log.warn("Não foi possível registar a chamada à IA: {}", e.getMessage());
            }
        });
    }
}
