package co.mz.ingenium.ai.core;

import java.time.ZoneId;

/**
 * Necessidade: textos e nomes repetidos em várias classes (chaves de metadados, nomes de
 * eventos SSE, nomes dos serviços) têm de ser IGUAIS em todo o lado. Um erro de escrita
 * ("tipo" vs "Tipo") partiria o RAG sem dar erro.
 * Funções: guardar todas as constantes partilhadas do módulo de IA.
 */
public final class AiConstants {

    private AiConstants() {
    }

    // ----- Nomes dos 7 serviços (gravados em ai_pedido_log) -----
    public static final String SERVICO_CHAT = "CAREER_CHAT";
    public static final String SERVICO_VAGAS = "JOB_RECOMMENDATION";
    public static final String SERVICO_PARCEIROS = "PARTNER_MATCHING";
    public static final String SERVICO_PERFIL = "PROFILE_IMPROVEMENT";
    public static final String SERVICO_POSTS = "POST_ANALYSIS";
    public static final String SERVICO_TRADUCAO = "TRANSLATION";
    public static final String SERVICO_APRENDIZAGEM = "LEARNING_ASSISTANT";

    // ----- Chaves dos metadados guardados com cada vetor no pgvector -----
    public static final String META_TIPO = "tipo";
    public static final String META_REFERENCIA = "referenciaId";
    public static final String META_TITULO = "titulo";
    public static final String META_CATEGORIA = "categoria";

    // ----- Nomes dos eventos SSE enviados ao frontend -----
    public static final String EVENTO_INICIO = "inicio";
    public static final String EVENTO_TOKEN = "token";
    public static final String EVENTO_FIM = "fim";
    public static final String EVENTO_ERRO = "erro";

    // ----- Tags -----
    // Similaridade mínima (0 a 1) para aceitar que um termo corresponde a uma tag
    public static final double LIMIAR_TAG = 0.5;

    // ----- Chat e textos -----
    public static final String TITULO_INICIAL = "Nova conversa";
    // Tamanho máximo de cada pedaço (chunk) de texto antes de virar vetor
    public static final int TAMANHO_PEDACO = 1000;
    public static final String RESPOSTA_VAZIA = "Desculpa, não consegui formular uma resposta. Podes reformular a pergunta?";
    public static final String ERRO_IA = "O Ingenium não conseguiu responder agora. Tenta novamente daqui a pouco.";

    // Fuso horário de Moçambique (para dizer a data certa à IA)
    public static final ZoneId FUSO = ZoneId.of("Africa/Maputo");
}
