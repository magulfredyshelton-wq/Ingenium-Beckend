package co.mz.ingenium.ai.config;

import co.mz.ingenium.ai.service.IndexacaoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 1) No arranque: lê src/main/resources/conhecimento/*.md e indexa cada ficheiro.
 *    O id é o nome do ficheiro; o título é a primeira linha "# Título" (ou o nome do ficheiro).
 * 2) Depois, de 5 em 5 minutos: sincroniza perfis e vagas (novos, alterados ou expirados).
 */
@Component
public class IndexacaoInicial implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(IndexacaoInicial.class);

    private final IndexacaoService indexacao;

    public IndexacaoInicial(IndexacaoService indexacao) {
        this.indexacao = indexacao;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            Resource[] ficheiros = new PathMatchingResourcePatternResolver().getResources("classpath*:conhecimento/*.md");
            for (Resource r : ficheiros) {
                String nome = r.getFilename() == null ? "sem-nome.md" : r.getFilename();
                String id = nome.replaceAll("\\.md$", "");
                String conteudo = new String(r.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                String titulo = conteudo.lines().findFirst()
                        .filter(l -> l.startsWith("# ")).map(l -> l.substring(2).strip()).orElse(id);
                indexacao.indexarConhecimento(id, titulo, "GERAL", conteudo);
            }
            log.info("Base de conhecimento: {} ficheiros verificados", ficheiros.length);
        } catch (Exception e) {
            // Não impede a aplicação de arrancar (ex.: falta de quota do Gemini)
            log.warn("Base de conhecimento não carregada: {}", e.getMessage());
        }
    }

    // Primeira vez 15 s depois de arrancar; depois 5 min depois de cada execução acabar
    @Scheduled(initialDelay = 15_000, fixedDelay = 300_000)
    public void sincronizar() {
        try {
            int n = indexacao.reindexarTudo();
            log.info("Sincronização de perfis e vagas: {} itens verificados", n);
        } catch (RuntimeException e) {
            log.warn("Sincronização falhou: {}", e.getMessage());
        }
    }
}