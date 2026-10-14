package co.mz.ingenium.ai.service;

import co.mz.ingenium.ai.config.IngeniumProperties;
import co.mz.ingenium.ai.model.TipoEntidade;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

import static co.mz.ingenium.ai.core.AiConstants.META_TIPO;

/**
 * Necessidade: é o "R" do RAG (Retrieval = recuperação). Vários serviços precisam de procurar
 * na base vetorial os textos mais parecidos com uma pergunta ou com um perfil.
 * Funções: pesquisar por similaridade no pgvector com filtros de metadados, ler a pontuação
 * de cada resultado e formatar os resultados como contexto para o prompt.
 */
@Service
public class PesquisaSemanticaService {

    private final VectorStore vectorStore;
    // Parâmetros por omissão (topK e limiar)
    private final IngeniumProperties.Rag rag;

    public PesquisaSemanticaService(VectorStore vectorStore, IngeniumProperties propriedades) {
        this.vectorStore = vectorStore;
        this.rag = propriedades.rag();
    }

    /**
     * Pesquisa genérica. Por dentro, o Spring AI: 1) gera o embedding do texto com o Gemini;
     * 2) faz no PostgreSQL "ORDER BY embedding <=> vetor" (distância do cosseno);
     * 3) aplica o filtro nos metadados; 4) devolve os topK mais próximos.
     */
    public List<Document> pesquisar(String texto, Filter.Expression filtro, int topK, double limiar) {
        // Começa a construir o pedido de pesquisa
        SearchRequest.Builder pedido = SearchRequest.builder()
                // Texto a comparar
                .query(texto)
                // Quantos resultados no máximo
                .topK(topK)
                // Similaridade mínima (0 = aceita tudo)
                .similarityThreshold(limiar);
        // O filtro é opcional
        if (filtro != null) {
            // Ex.: tipo == 'VAGA'
            pedido.filterExpression(filtro);
        }
        // Executa a pesquisa
        return vectorStore.similaritySearch(pedido.build());
    }

    /** Pesquisa com os valores do properties, limitada a certos tipos (ex.: CONHECIMENTO, VAGA). */
    public List<Document> pesquisarPorTipos(String texto, TipoEntidade... tipos) {
        // Construtor de filtros do Spring AI
        FilterExpressionBuilder b = new FilterExpressionBuilder();
        // Converte os enums em texto: [CONHECIMENTO, VAGA] -> ["CONHECIMENTO", "VAGA"]
        Object[] nomes = Arrays.stream(tipos).map(Enum::name).toArray();
        // tipo IN (...)
        Filter.Expression filtro = b.in(META_TIPO, nomes).build();
        // Usa topK e limiar configurados
        return pesquisar(texto, filtro, rag.topK(), rag.limiarSimilaridade());
    }

    /** Similaridade de um resultado (0..1). O PgVectorStore guarda-a em getScore(). */
    public static double pontuacao(Document documento) {
        // Pontuação calculada pelo Spring AI (1 - distância)
        Double score = documento.getScore();
        // Se existir, usa-a
        if (score != null) {
            return score;
        }
        // Alternativa: a distância guardada nos metadados
        Object distancia = documento.getMetadata().get("distance");
        // Converte distância em similaridade
        if (distancia instanceof Number numero) {
            return 1.0 - numero.doubleValue();
        }
        return 0.0;
    }

    /** Transforma os resultados num bloco de texto numerado para o prompt. */
    public static String formatarContexto(List<Document> documentos) {
        // Sem resultados, dizemos isso explicitamente à IA
        if (documentos.isEmpty()) {
            return "(nenhuma informação relevante encontrada)";
        }
        StringBuilder sb = new StringBuilder();
        // Numeração [1], [2], ...
        int i = 1;
        // Para cada pedaço recuperado...
        for (Document d : documentos) {
            // ...acrescenta "[n] texto" e uma linha em branco
            sb.append('[').append(i++).append("] ").append(d.getText()).append("\n\n");
        }
        return sb.toString().strip();
    }
}
