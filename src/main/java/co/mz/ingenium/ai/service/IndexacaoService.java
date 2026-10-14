package co.mz.ingenium.ai.service;

import co.mz.ingenium.ai.model.EntidadeIndexada;
import co.mz.ingenium.ai.model.Tag;
import co.mz.ingenium.ai.model.TipoEntidade;
import co.mz.ingenium.ai.repository.EntidadeIndexadaRepository;
import co.mz.ingenium.ai.repository.TagRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static co.mz.ingenium.ai.core.AiConstants.*;

/**
 * INDEXAÇÃO do RAG: lê perfis e vagas da base de dados, transforma-os em texto, parte em
 * pedaços, gera embeddings (Gemini) e grava no pgvector. Um hash SHA-256 evita pagar
 * embeddings quando nada mudou. As TAGS vêm das tabelas do backend.
 */
@Service
public class IndexacaoService {

    private static final Logger log = LoggerFactory.getLogger(IndexacaoService.class);

    private final VectorStore vectorStore;
    private final EntidadeIndexadaRepository repo;
    private final TagRepository tagRepo;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;

    public IndexacaoService(VectorStore vectorStore, EntidadeIndexadaRepository repo, TagRepository tagRepo,
                            JdbcTemplate jdbc, TransactionTemplate tx) {
        this.vectorStore = vectorStore;
        this.repo = repo;
        this.tagRepo = tagRepo;
        this.jdbc = jdbc;
        this.tx = tx;
    }

    /** Indexa (ou atualiza) o perfil de um estudante, lido das tabelas students, user_tag e course_tag. */
    @Transactional
    public void indexarPerfil(UUID estudanteId) {
        List<Map<String, Object>> linhas = jdbc.queryForList("""
                SELECT s.full_name, c.name AS curso, s.seeking
                FROM students s LEFT JOIN course c ON c.id = s.course_id
                WHERE s.id = ?
                """, estudanteId);
        if (linhas.isEmpty()) {
            removerSeExistir(TipoEntidade.PERFIL, estudanteId.toString());
            return;
        }
        Map<String, Object> p = linhas.get(0);
        // Tags escolhidas pelo estudante + tags do curso dele
        List<UUID> idsTags = jdbc.queryForList(
                "SELECT tag_id FROM user_tag WHERE user_id = ?", UUID.class, estudanteId);
        // As tags do curso entram só no texto (para a similaridade), não na cobertura
        List<String> tagsCurso = jdbc.queryForList("""
                SELECT t.name FROM course_tag ct
                JOIN tag t ON t.id = ct.tag_id
                JOIN students s ON s.course_id = ct.course_id
                WHERE s.id = ? AND t.active
                """, String.class, estudanteId);
        // Tags-mãe (áreas) que ele segue
        List<String> areas = jdbc.queryForList("""
                SELECT c.name FROM user_tag_category u
                JOIN tag_category c ON c.id = u.tag_category_id WHERE u.user_id = ?
                """, String.class, estudanteId);
        Set<Tag> tags = ativas(idsTags);

        StringBuilder t = new StringBuilder();
        linha(t, "Estudante", (String) p.get("full_name"));
        linha(t, "Curso", (String) p.get("curso"));
        linha(t, "Procura", tipo((String) p.get("seeking")));
        lista(t, "Áreas de interesse", areas);
        lista(t, "Tags", tags.stream().map(Tag::getName).toList());
        lista(t, "Temas do curso", tagsCurso);
        indexar(TipoEntidade.PERFIL, estudanteId.toString(), (String) p.get("full_name"), t.toString(), tags, true, null);
    }

    /** Indexa (ou atualiza/fecha) uma vaga, lida das tabelas job, post, company e post_tag. */
    @Transactional
    public void indexarVaga(UUID postId) {
        List<Map<String, Object>> linhas = jdbc.queryForList("""
                SELECT j.title, j.description, j.work_type, j.application_end, c.company_name,
                       (j.status = 'OPEN' AND j.application_end >= CURRENT_DATE AND p.deleted_at IS NULL) AS aberta
                FROM job j
                JOIN post p ON p.id = j.post_id
                JOIN company c ON c.id = j.company_id
                WHERE j.post_id = ?
                """, postId);
        if (linhas.isEmpty()) {
            removerSeExistir(TipoEntidade.VAGA, postId.toString());
            return;
        }
        Map<String, Object> v = linhas.get(0);
        List<UUID> idsTags = jdbc.queryForList("SELECT tag_id FROM post_tag WHERE post_id = ?", UUID.class, postId);
        Set<Tag> tags = ativas(idsTags);
        boolean aberta = Boolean.TRUE.equals(v.get("aberta"));

        StringBuilder t = new StringBuilder();
        linha(t, "Vaga", (String) v.get("title"));
        linha(t, "Empresa", (String) v.get("company_name"));
        linha(t, "Tipo", tipo((String) v.get("work_type")));
        linha(t, "Candidaturas até", String.valueOf(v.get("application_end")));
        linha(t, "Descrição", (String) v.get("description"));
        lista(t, "Tags", tags.stream().map(Tag::getName).toList());
        indexar(TipoEntidade.VAGA, postId.toString(), (String) v.get("title"), t.toString(), tags, aberta,
                (String) v.get("work_type"));
    }

    /** Indexa um texto da base de conhecimento (vem dos ficheiros .md). */
    @Transactional
    public void indexarConhecimento(String id, String titulo, String categoria, String conteudo) {
        indexar(TipoEntidade.CONHECIMENTO, id, titulo, conteudo, Set.of(), true, categoria);
    }

    /** Sincroniza todos os perfis e vagas. Barato quando nada mudou (só compara hashes). */
    public int reindexarTudo() {
        int feitos = 0;
        for (UUID id : jdbc.queryForList("SELECT id FROM students", UUID.class)) {
            if (executar(() -> indexarPerfil(id), "perfil " + id)) feitos++;
        }
        for (UUID id : jdbc.queryForList("SELECT post_id FROM job", UUID.class)) {
            if (executar(() -> indexarVaga(id), "vaga " + id)) feitos++;
        }
        return feitos;
    }

    // Cada item numa transação própria: se um falha, os outros continuam
    private boolean executar(Runnable tarefa, String descricao) {
        try {
            tx.executeWithoutResult(s -> tarefa.run());
            return true;
        } catch (RuntimeException e) {
            log.warn("Não foi possível indexar {}: {}", descricao, e.getMessage());
            return false;
        }
    }

    // ===================================================================
    //  NÚCLEO DA INDEXAÇÃO
    // ===================================================================

    private void indexar(TipoEntidade tipo, String ref, String titulo, String texto,
                         Set<Tag> tags, boolean ativa, String categoria) {
        String hash = sha256(texto + "|" + ativa);
        EntidadeIndexada e = repo.findByTipoAndReferenciaId(tipo, ref).orElseGet(() -> new EntidadeIndexada(tipo, ref));
        boolean mudou = !hash.equals(e.getHashConteudo());
        e.atualizar(cortar(titulo, 255), texto, hash, ativa, tags);
        repo.save(e);
        // Só gastamos embeddings se o texto mudou
        if (mudou) {
            removerVetores(tipo, ref);
            if (ativa) {
                vectorStore.add(criarDocumentos(tipo, ref, titulo, partir(texto), categoria));
            }
        }
    }

    private Set<Tag> ativas(List<UUID> ids) {
        Set<Tag> tags = new LinkedHashSet<>();
        for (Tag t : tagRepo.findAllById(ids)) {
            if (t.isActive()) tags.add(t);
        }
        return tags;
    }

    private void removerSeExistir(TipoEntidade tipo, String ref) {
        repo.findByTipoAndReferenciaId(tipo, ref).ifPresent(e -> {
            removerVetores(tipo, ref);
            repo.delete(e);
        });
    }

    private List<Document> criarDocumentos(TipoEntidade tipo, String ref, String titulo, List<String> pedacos, String categoria) {
        String cabecalho = "[" + tipo.name() + "] " + (titulo == null ? "" : titulo) + "\n";
        List<Document> documentos = new ArrayList<>();
        for (String pedaco : pedacos) {
            Map<String, Object> meta = new HashMap<>();
            meta.put(META_TIPO, tipo.name());
            meta.put(META_REFERENCIA, ref);
            meta.put(META_TITULO, titulo == null ? "" : titulo);
            if (categoria != null) {
                meta.put(META_CATEGORIA, categoria);
            }
            documentos.add(new Document(cabecalho + pedaco, meta));
        }
        return documentos;
    }

    private void removerVetores(TipoEntidade tipo, String ref) {
        FilterExpressionBuilder b = new FilterExpressionBuilder();
        vectorStore.delete(b.and(b.eq(META_TIPO, tipo.name()), b.eq(META_REFERENCIA, ref)).build());
    }

    /** CHUNKING: junta parágrafos até ~1000 caracteres; parágrafos maiores são cortados no último espaço. */
    static List<String> partir(String texto) {
        List<String> pedacos = new ArrayList<>();
        StringBuilder atual = new StringBuilder();
        for (String paragrafo : texto.split("\\n\\s*\\n")) {
            String p = paragrafo.strip();
            if (p.isEmpty()) {
                continue;
            }
            while (p.length() > TAMANHO_PEDACO) {
                int corte = p.lastIndexOf(' ', TAMANHO_PEDACO);
                if (corte <= 0) {
                    corte = TAMANHO_PEDACO;
                }
                fechar(pedacos, atual);
                pedacos.add(p.substring(0, corte).strip());
                p = p.substring(corte).strip();
            }
            if (atual.length() + p.length() + 2 > TAMANHO_PEDACO) {
                fechar(pedacos, atual);
            }
            if (!atual.isEmpty()) {
                atual.append("\n\n");
            }
            atual.append(p);
        }
        fechar(pedacos, atual);
        return pedacos;
    }

    private static void fechar(List<String> pedacos, StringBuilder atual) {
        if (!atual.isEmpty()) {
            pedacos.add(atual.toString());
            atual.setLength(0);
        }
    }

    private static String tipo(String valor) {
        if (valor == null) return null;
        return switch (valor) {
            case "INTERNSHIP" -> "estágio";
            case "JOB" -> "emprego";
            default -> "estágio ou emprego";
        };
    }

    private static void linha(StringBuilder t, String rotulo, String valor) {
        if (valor != null && !valor.isBlank() && !"null".equals(valor)) {
            t.append(rotulo).append(": ").append(valor.strip()).append('\n');
        }
    }

    private static void lista(StringBuilder t, String rotulo, List<String> valores) {
        if (valores != null && !valores.isEmpty()) {
            linha(t, rotulo, String.join(", ", valores));
        }
    }

    private static String cortar(String s, int max) {
        return s == null ? null : (s.length() > max ? s.substring(0, max) : s);
    }

    private static String sha256(String texto) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(texto.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}