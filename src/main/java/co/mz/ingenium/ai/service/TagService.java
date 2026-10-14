package co.mz.ingenium.ai.service;

import co.mz.ingenium.ai.model.Tag;
import co.mz.ingenium.ai.repository.EntidadeIndexadaRepository;
import co.mz.ingenium.ai.repository.TagRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;

import static co.mz.ingenium.ai.core.AiConstants.LIMIAR_TAG;

/**
 * Liga o que as pessoas escrevem ("#DesenvolvimentoWeb", "Cibersegurança") às tags reais
 * da base de dados. A IA nunca inventa tags: só usa as que existem.
 */
@Service
public class TagService {

    // Semelhança entre slugs (pg_trgm): apanha pequenos erros de escrita
    private static final String SQL_APROXIMADO = """
            SELECT id FROM tag
            WHERE active AND similarity(slug, ?) >= ?
            ORDER BY similarity(slug, ?) DESC
            LIMIT 1
            """;

    private final TagRepository repo;
    private final EntidadeIndexadaRepository entidades;
    private final JdbcTemplate jdbc;

    public TagService(TagRepository repo, EntidadeIndexadaRepository entidades, JdbcTemplate jdbc) {
        this.repo = repo;
        this.entidades = entidades;
        this.jdbc = jdbc;
    }

    /** "#Desenvolvimento Web" -> "desenvolvimento-web"; "Cibersegurança" -> "ciberseguranca". */
    public static String slugificar(String texto) {
        if (texto == null) {
            return "";
        }
        // Tira o # das hashtags (só no início de palavra, para manter "c#")
        String s = texto.replaceAll("(^|\\s)#", "$1");
        // camelCase -> palavras separadas ("DesenvolvimentoWeb" -> "Desenvolvimento Web")
        s = s.replaceAll("(\\p{Ll})(\\p{Lu})", "$1 $2");
        // Remove acentos
        s = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        s = s.toLowerCase(Locale.ROOT);
        // Tudo o que não é letra/número/+/#/. vira hífen
        s = s.replaceAll("[^a-z0-9+#.]+", "-");
        return s.replaceAll("^-+|-+$", "");
    }

    /** Resolve um termo para UMA tag da base de dados: 1) slug exato; 2) o mais parecido. */
    @Transactional(readOnly = true)
    public Optional<Tag> resolver(String termo) {
        String slug = slugificar(termo);
        if (slug.isBlank() || slug.length() > 80) {
            return Optional.empty();
        }
        Optional<Tag> exata = repo.findBySlugAndActiveTrue(slug);
        if (exata.isPresent()) {
            return exata;
        }
        List<UUID> ids = jdbc.query(SQL_APROXIMADO, (rs, i) -> rs.getObject(1, UUID.class),
                slug, LIMIAR_TAG, slug);
        return ids.isEmpty() ? Optional.empty() : repo.findById(ids.get(0));
    }

    /** Resolve vários termos, sem repetidos. */
    @Transactional(readOnly = true)
    public Set<Tag> resolverTodos(Collection<String> termos) {
        Set<Tag> tags = new LinkedHashSet<>();
        if (termos != null) {
            for (String termo : termos) {
                resolver(termo).ifPresent(tags::add);
            }
        }
        return tags;
    }

    /** Lista de tags por tag-mãe, em texto, para meter nos prompts do Gemini. */
    @Transactional(readOnly = true)
    public String catalogoParaPrompt() {
        Map<String, List<String>> porArea = new TreeMap<>();
        for (Tag t : repo.findByActiveTrueOrderByNameAsc()) {
            porArea.computeIfAbsent(t.getCategoriaNome(), k -> new ArrayList<>()).add(t.getSlug());
        }
        return porArea.entrySet().stream()
                .map(e -> "- " + e.getKey() + ": " + String.join(", ", e.getValue()))
                .collect(Collectors.joining("\n"));
    }

    /** Tags mais pedidas nas vagas ativas. */
    @Transactional(readOnly = true)
    public List<String> competenciasMaisPedidas(int limite) {
        return entidades.competenciasMaisPedidas(Math.max(1, Math.min(limite, 50)));
    }
}