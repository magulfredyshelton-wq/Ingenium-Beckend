package co.mz.ingenium.ai.service;

import co.mz.ingenium.ai.core.AiPromptExecutor;
import co.mz.ingenium.ai.core.AiRequestContext;
import co.mz.ingenium.ai.core.IngeniumPersona;
import co.mz.ingenium.ai.dto.RecomendacaoDtos.Explicacao;
import co.mz.ingenium.ai.dto.RecomendacaoDtos.Explicacoes;
import co.mz.ingenium.ai.dto.RecomendacaoDtos.HistoricoItem;
import co.mz.ingenium.ai.dto.RecomendacaoDtos.VagaRecomendada;
import co.mz.ingenium.ai.exception.IngeniumException;
import co.mz.ingenium.ai.exception.RecursoNaoEncontradoException;
import co.mz.ingenium.ai.model.EntidadeIndexada;
import co.mz.ingenium.ai.model.Recomendacao;
import co.mz.ingenium.ai.model.Tag;
import co.mz.ingenium.ai.model.TipoEntidade;
import co.mz.ingenium.ai.repository.EntidadeIndexadaRepository;
import co.mz.ingenium.ai.repository.RecomendacaoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static co.mz.ingenium.ai.core.AiConstants.*;

/**
 * Necessidade: recomendar vagas é um problema de ORDENAÇÃO, não de conversa. Por isso a ordem é
 * calculada com números (similaridade dos embeddings + cobertura de competências da taxonomia)
 * e a LLM só entra no fim, para EXPLICAR — se o Gemini falhar, as recomendações continuam a sair.
 * Funções: recomendar vagas a um estudante, explicar cada recomendação, guardar o histórico.
 */
@Service
public class JobRecommendationService {

    private static final Logger log = LoggerFactory.getLogger(JobRecommendationService.class);
    // Peso da similaridade semântica na pontuação final
    private static final double PESO_SIMILARIDADE = 0.65;
    // Peso da cobertura de competências
    private static final double PESO_COBERTURA = 0.35;
    // Quantos pedaços de vagas pedir ao pgvector antes de ordenar
    private static final int CANDIDATOS = 40;
    // Crédito dado a uma tag que o estudante não tem, mas que é da mesma tag-mãe (ex.: html vs desenvolvimento-web)
    private static final double PESO_MESMA_AREA = 0.4;

    private final PesquisaSemanticaService pesquisa;
    private final EntidadeIndexadaRepository entidades;
    private final RecomendacaoRepository historico;
    private final AiPromptExecutor executor;
    private final IngeniumPersona persona;

    public JobRecommendationService(PesquisaSemanticaService pesquisa, EntidadeIndexadaRepository entidades,
                                    RecomendacaoRepository historico, AiPromptExecutor executor,
                                    IngeniumPersona persona) {
        this.pesquisa = pesquisa;
        this.entidades = entidades;
        this.historico = historico;
        this.executor = executor;
        this.persona = persona;
    }

    /** Recomenda até "limite" vagas; se explicar=true pede ao Ingenium uma frase por vaga. */
    public List<VagaRecomendada> recomendar(String utilizadorId, int limite, boolean explicar) {
        // Limite entre 1 e 20
        int n = Math.max(1, Math.min(limite, 20));
        // Perfil do estudante já com os conceitos
        EntidadeIndexada perfil = carregarPerfil(utilizadorId);
        // Filtro: só vetores de VAGAS
        FilterExpressionBuilder b = new FilterExpressionBuilder();
        // Pesquisa semântica: vagas cujo texto se parece com o perfil (limiar 0 = aceita todas)
        List<Document> docs = pesquisa.pesquisar(perfil.getTexto(), b.eq(META_TIPO, TipoEntidade.VAGA.name()).build(), CANDIDATOS, 0.0);
        // Uma vaga pode ter vários pedaços: fica a melhor pontuação de cada
        Map<String, Double> melhorPorVaga = new LinkedHashMap<>();
        for (Document d : docs) {
            melhorPorVaga.merge(String.valueOf(d.getMetadata().get(META_REFERENCIA)), PesquisaSemanticaService.pontuacao(d), Math::max);
        }
        // Sem vagas indexadas
        if (melhorPorVaga.isEmpty()) {
            return List.of();
        }
        // Carrega as vagas com os conceitos numa só consulta (só as ativas)
        Map<String, EntidadeIndexada> vagas = entidades.comTags(TipoEntidade.VAGA, melhorPorVaga.keySet()).stream()
                .filter(EntidadeIndexada::isAtiva)
                .collect(Collectors.toMap(EntidadeIndexada::getReferenciaId, Function.identity()));
        // Ids dos conceitos que o estudante tem
        Set<UUID> meus = perfil.getTags().stream().map(Tag::getId).collect(Collectors.toSet());
        Set<UUID> minhasAreas = perfil.getTags().stream().map(Tag::getCategoriaId).collect(Collectors.toSet());
        // Lista de candidatas com pontuação
        List<VagaRecomendada> lista = new ArrayList<>();
        for (Map.Entry<String, Double> entrada : melhorPorVaga.entrySet()) {
            // Vaga correspondente (pode ter sido fechada)
            EntidadeIndexada vaga = vagas.get(entrada.getKey());
            if (vaga == null) {
                continue;
            }
            // Competências pedidas pela vaga
            Set<Tag> pedidas = vaga.getTags();
            // As que o estudante já tem
            List<String> comum = pedidas.stream().filter(c -> meus.contains(c.getId())).map(Tag::getName).sorted().toList();
            // As que lhe faltam
            List<String> falta = pedidas.stream().filter(c -> !meus.contains(c.getId())).map(Tag::getName).sorted().toList();
            // Cobertura = tenho / pedidas
            long mesmaArea = pedidas.stream()
                    .filter(t -> !meus.contains(t.getId()) && minhasAreas.contains(t.getCategoriaId())).count();
            double cobertura = pedidas.isEmpty() ? 0.0 : (comum.size() + PESO_MESMA_AREA * mesmaArea) / pedidas.size();
            // Similaridade semântica
            double similaridade = entrada.getValue();
            // Sem competências na vaga, conta só a similaridade
            double pontuacao = pedidas.isEmpty() ? similaridade : PESO_SIMILARIDADE * similaridade + PESO_COBERTURA * cobertura;
            // Guarda a candidata
            lista.add(new VagaRecomendada(vaga.getReferenciaId(), vaga.getTitulo(), arredondar(pontuacao),
                    arredondar(similaridade), arredondar(cobertura), comum, falta, null));
        }
        // Ordena da maior para a menor pontuação
        lista.sort(Comparator.comparingDouble(VagaRecomendada::pontuacao).reversed());
        // Fica com as n primeiras
        List<VagaRecomendada> top = new ArrayList<>(lista.subList(0, Math.min(n, lista.size())));
        // Explicações (opcional)
        if (explicar && !top.isEmpty()) {
            top = comExplicacoes(utilizadorId, perfil, top, vagas);
        }
        // Histórico
        guardarHistorico(utilizadorId, top);
        return top;
    }

    /** Últimas recomendações feitas ao estudante. */
    public List<HistoricoItem> historico(String utilizadorId) {
        return historico.findTop20ByUtilizadorIdOrderByCriadaEmDesc(utilizadorId).stream()
                .map(r -> new HistoricoItem(r.getTipo(), r.getAlvoId(), r.getPontuacao(), r.getExplicacao(), r.getCriadaEm()))
                .toList();
    }

    // Pede ao Gemini uma explicação curta por vaga (se falhar, devolve sem explicação)
    private List<VagaRecomendada> comExplicacoes(String utilizadorId, EntidadeIndexada perfil,
                                                 List<VagaRecomendada> top, Map<String, EntidadeIndexada> vagas) {
        // Instruções
        String sistema = persona.identidade() + """

                TAREFA: explicar ao estudante, em 1 ou 2 frases por vaga, porque cada vaga lhe foi \
                recomendada. Usa as competências em comum e sê honesto sobre o que falta, sugerindo \
                como o conseguir. Usa exatamente o mesmo "id" de cada vaga.
                """;
        // Dados: perfil + vagas
        StringBuilder dados = new StringBuilder("PERFIL DO ESTUDANTE:\n").append(perfil.getTexto()).append("\n\nVAGAS:\n");
        for (VagaRecomendada v : top) {
            String texto = vagas.get(v.vagaId()).getTexto();
            dados.append("- id: ").append(v.vagaId()).append('\n')
                    .append("  descrição: ").append(texto.length() > 700 ? texto.substring(0, 700) : texto).append('\n')
                    .append("  competências em comum: ").append(v.competenciasEmComum()).append('\n')
                    .append("  competências em falta: ").append(v.competenciasEmFalta()).append('\n');
        }
        try {
            // Resposta estruturada: lista de {id, texto}
            Explicacoes resposta = executor.executarEstruturado(
                    AiRequestContext.iniciar(utilizadorId, SERVICO_VAGAS), sistema, dados.toString(), Explicacoes.class);
            // Mapa id -> texto
            Map<String, String> porId = new HashMap<>();
            if (resposta.itens() != null) {
                for (Explicacao e : resposta.itens()) {
                    if (e != null && e.id() != null && e.texto() != null) porId.put(e.id().trim(), e.texto().strip());
                }
            }
            // Recria cada recomendação com a explicação
            return top.stream().map(v -> new VagaRecomendada(v.vagaId(), v.titulo(), v.pontuacao(), v.similaridade(),
                    v.coberturaCompetencias(), v.competenciasEmComum(), v.competenciasEmFalta(), porId.get(v.vagaId()))).toList();
        } catch (IngeniumException e) {
            // A LLM está fora do caminho crítico: continua sem explicações
            log.warn("Recomendações sem explicação: {}", e.getMessage());
            return top;
        }
    }

    // Grava o histórico (uma falha aqui não estraga a resposta)
    private void guardarHistorico(String utilizadorId, List<VagaRecomendada> top) {
        try {
            historico.saveAll(top.stream()
                    .map(v -> new Recomendacao(utilizadorId, "VAGA", v.vagaId(), v.pontuacao(), v.explicacao()))
                    .toList());
        } catch (RuntimeException e) {
            log.warn("Não foi possível guardar o histórico: {}", e.getMessage());
        }
    }

    // Perfil com conceitos ou 404
    private EntidadeIndexada carregarPerfil(String utilizadorId) {
        return entidades.comTags(TipoEntidade.PERFIL, List.of(utilizadorId)).stream().findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "O teu perfil ainda não foi enviado ao Ingenium. Completa o perfil e tenta de novo."));
    }

    // 3 casas decimais
    static double arredondar(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }
}
