package co.mz.ingenium.ai.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Necessidade: o módulo de IA não lê diretamente a BD do backend principal. O backend envia-lhe
 * perfis e vagas, e o módulo guarda aqui uma cópia de trabalho: o texto que virou vetores,
 * um hash (para não gerar embeddings repetidos) e os conceitos da taxonomia associados.
 * Funções: representar uma linha de ai_entidade_indexada e a ligação N:N com a taxonomia.
 */
@Entity
@Table(name = "ai_entidade_indexada")
public class EntidadeIndexada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // PERFIL, VAGA ou CONHECIMENTO
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoEntidade tipo;

    // Id do registo no backend principal (id do estudante, id da vaga, ...)
    @Column(name = "referencia_id", nullable = false, length = 64)
    private String referenciaId;

    // Título legível (nome do estudante, título da vaga, ...)
    private String titulo;

    // Texto completo que foi transformado em vetores
    @Column(nullable = false, columnDefinition = "TEXT")
    private String texto;

    // SHA-256 do texto: se não mudar, não gastamos embeddings outra vez
    @Column(name = "hash_conteudo", nullable = false, length = 64)
    private String hashConteudo;

    // false = vaga fechada (deixa de ser recomendada)
    @Column(nullable = false)
    private boolean ativa = true;

    // Última atualização
    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm = LocalDateTime.now();

    // Ligação N:N com a taxonomia, através da tabela ai_entidade_conceito
    @ManyToMany
    @JoinTable(name = "ai_entidade_tag",
            joinColumns = @JoinColumn(name = "entidade_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<Tag> tags = new HashSet<>();

    protected EntidadeIndexada() {
    }

    public EntidadeIndexada(TipoEntidade tipo, String referenciaId) {
        this.tipo = tipo;
        this.referenciaId = referenciaId;
    }

    // Atualiza o conteúdo e a data de uma só vez
    public void atualizar(String titulo, String texto, String hash, boolean ativa, Set<Tag> tags) {
        this.titulo = titulo;
        this.texto = texto;
        this.hashConteudo = hash;
        this.ativa = ativa;
        this.tags.clear();
        this.tags.addAll(tags);
        this.atualizadoEm = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public TipoEntidade getTipo() { return tipo; }
    public String getReferenciaId() { return referenciaId; }
    public String getTitulo() { return titulo; }
    public String getTexto() { return texto; }
    public String getHashConteudo() { return hashConteudo; }
    public boolean isAtiva() { return ativa; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public Set<Tag> getTags() { return tags; }
}
