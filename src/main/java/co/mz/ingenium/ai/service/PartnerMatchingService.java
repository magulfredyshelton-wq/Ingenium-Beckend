package co.mz.ingenium.ai.service;

import co.mz.ingenium.ai.core.AiPromptExecutor;
import co.mz.ingenium.ai.core.AiRequestContext;
import co.mz.ingenium.ai.core.IngeniumPersona;
import co.mz.ingenium.ai.dto.RecomendacaoDtos.Explicacao;
import co.mz.ingenium.ai.dto.RecomendacaoDtos.Explicacoes;
import co.mz.ingenium.ai.dto.RecomendacaoDtos.ParceiroRecomendado;
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
 * Necessidade: o Ingenium ajuda a formar equipas e parcerias. Um bom parceiro tem interesses
 * parecidos (similaridade) mas competências que eu NÃO tenho (complementaridade).
 * Funções: encontrar colegas compatíveis, calcular as competências complementares e em comum,
 * explicar cada sugestão e guardar o histórico.
 */
@Service
public class PartnerMatchingService {

    private static final Logger log = LoggerFactory.getLogger(PartnerMatchingService.class);
    // Pesos da pontuação final
    private static final double PESO_SIMILARIDADE = 0.6;
    private static final double PESO_COMPLEMENTO = 0.4;
    // Candidatos pedidos ao pgvector
    private static final int CANDIDATOS = 40;

    private final PesquisaSemanticaService pesquisa;
    private final EntidadeIndexadaRepository entidades;
    private final RecomendacaoRepository historico;
    private final AiPromptExecutor executor;
    private final IngeniumPersona persona;

    public PartnerMatchingService(PesquisaSemanticaService pesquisa, EntidadeIndexadaRepository entidades,
                                  RecomendacaoRepository historico, AiPromptExecutor executor, IngeniumPersona persona) {
        this.pesquisa = pesquisa;
        this.entidades = entidades;
        this.historico = historico;
        this.executor = executor;
        this.persona = persona;
    }

    /** Sugere colegas; "objetivo" descreve o projeto/equipa (ex.: "app de reciclagem"). */
    public List<ParceiroRecomendado> recomendar(String utilizadorId, String objetivo, int limite, boolean explicar) {
        // Limite entre 1 e 20
        int n = Math.max(1, Math.min(limite, 20));
        // O meu perfil com conceitos
        EntidadeIndexada eu = entidades.comTags(TipoEntidade.PERFIL, List.of(utilizadorId)).stream().findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException("O teu perfil ainda não foi enviado ao Ingenium."));
        // Texto de pesquisa: objetivo (se houver) + o meu perfil
        String consulta = (objetivo == null || objetivo.isBlank() ? "" : "Projeto/objetivo: " + objetivo + "\n") + eu.getTexto();
        // Filtro: perfis que não sejam o meu
        FilterExpressionBuilder b = new FilterExpressionBuilder();
        List<Document> docs = pesquisa.pesquisar(consulta,
                b.and(b.eq(META_TIPO, TipoEntidade.PERFIL.name()), b.ne(META_REFERENCIA, utilizadorId)).build(),
                CANDIDATOS, 0.0);
        // Melhor pontuação por colega
        Map<String, Double> melhor = new LinkedHashMap<>();
        for (Document d : docs) {
            melhor.merge(String.valueOf(d.getMetadata().get(META_REFERENCIA)), PesquisaSemanticaService.pontuacao(d), Math::max);
        }
        // Ninguém encontrado
        if (melhor.isEmpty()) {
            return List.of();
        }
        // Perfis dos colegas com conceitos
        Map<String, EntidadeIndexada> colegas = entidades.comTags(TipoEntidade.PERFIL, melhor.keySet()).stream()
                .collect(Collectors.toMap(EntidadeIndexada::getReferenciaId, Function.identity()));
        // As minhas competências
        Set<UUID> minhas = eu.getTags().stream().map(Tag::getId).collect(Collectors.toSet());
        // Candidatos
        List<ParceiroRecomendado> lista = new ArrayList<>();
        for (Map.Entry<String, Double> e : melhor.entrySet()) {
            EntidadeIndexada colega = colegas.get(e.getKey());
            if (colega == null) {
                continue;
            }
            // Competências do colega
            Set<Tag> deles = colega.getTags();
            // O que ele tem e eu não
            List<String> complementares = deles.stream().filter(c -> !minhas.contains(c.getId())).map(Tag::getName).sorted().toList();
            // O que temos os dois
            List<String> comum = deles.stream().filter(c -> minhas.contains(c.getId())).map(Tag::getName).sorted().toList();
            // Complementaridade 0..1
            double complemento = deles.isEmpty() ? 0.0 : (double) complementares.size() / deles.size();
            // Pontuação final
            double pontuacao = deles.isEmpty() ? e.getValue() : PESO_SIMILARIDADE * e.getValue() + PESO_COMPLEMENTO * complemento;
            lista.add(new ParceiroRecomendado(colega.getReferenciaId(), colega.getTitulo(),
                    JobRecommendationService.arredondar(pontuacao), JobRecommendationService.arredondar(e.getValue()),
                    complementares, comum, null));
        }
        // Ordena e corta
        lista.sort(Comparator.comparingDouble(ParceiroRecomendado::pontuacao).reversed());
        List<ParceiroRecomendado> top = new ArrayList<>(lista.subList(0, Math.min(n, lista.size())));
        // Explicações opcionais
        if (explicar && !top.isEmpty()) {
            top = comExplicacoes(utilizadorId, objetivo, eu, top);
        }
        try {
            historico.saveAll(top.stream().map(p -> new Recomendacao(utilizadorId, "PARCEIRO", p.utilizadorId(), p.pontuacao(), p.explicacao())).toList());
        } catch (RuntimeException ex) {
            log.warn("Histórico de parceiros não gravado: {}", ex.getMessage());
        }
        return top;
    }

    // Explicação curta por colega (sem expor dados sensíveis)
    private List<ParceiroRecomendado> comExplicacoes(String utilizadorId, String objetivo, EntidadeIndexada eu,
                                                     List<ParceiroRecomendado> top) {
        String sistema = persona.identidade() + """

                TAREFA: para cada colega, explica em 1 ou 2 frases porque seria um bom parceiro de \
                equipa para o estudante, destacando as competências complementares. Usa o mesmo "id".
                """;
        StringBuilder dados = new StringBuilder("MEU PERFIL:\n").append(eu.getTexto())
                .append("\nOBJETIVO: ").append(objetivo == null ? "(não indicado)" : objetivo).append("\n\nCOLEGAS:\n");
        for (ParceiroRecomendado p : top) {
            dados.append("- id: ").append(p.utilizadorId()).append(" | nome: ").append(p.nome())
                    .append(" | complementares: ").append(p.competenciasComplementares())
                    .append(" | em comum: ").append(p.competenciasEmComum()).append('\n');
        }
        try {
            Explicacoes r = executor.executarEstruturado(AiRequestContext.iniciar(utilizadorId, SERVICO_PARCEIROS),
                    sistema, dados.toString(), Explicacoes.class);
            Map<String, String> porId = new HashMap<>();
            if (r.itens() != null) {
                for (Explicacao e : r.itens()) {
                    if (e != null && e.id() != null && e.texto() != null) porId.put(e.id().trim(), e.texto().strip());
                }
            }
            return top.stream().map(p -> new ParceiroRecomendado(p.utilizadorId(), p.nome(), p.pontuacao(), p.similaridade(),
                    p.competenciasComplementares(), p.competenciasEmComum(), porId.get(p.utilizadorId()))).toList();
        } catch (IngeniumException e) {
            log.warn("Parceiros sem explicação: {}", e.getMessage());
            return top;
        }
    }
}
