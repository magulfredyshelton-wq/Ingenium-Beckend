package co.mz.ingenium.ai.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Necessidade: o estudante pode ter várias conversas com o Ingenium e voltar a elas.
 * Funções: representar uma linha da tabela ai_conversa (dono, título e datas).
 */
// Classe persistida pelo JPA
@Entity
// Nome da tabela na base de dados
@Table(name = "ai_conversa")
public class Conversa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Id do estudante no backend principal (texto: serve para Long ou UUID)
    @Column(name = "utilizador_id", nullable = false, length = 64)
    private String utilizadorId;

    // Título mostrado na lista de conversas
    @Column(nullable = false, length = 120)
    private String titulo;

    // Data de criação (não muda depois)
    @Column(name = "criada_em", nullable = false, updatable = false)
    private LocalDateTime criadaEm = LocalDateTime.now();

    // Data da última mensagem (para ordenar a lista)
    @Column(name = "atualizada_em", nullable = false)
    private LocalDateTime atualizadaEm = LocalDateTime.now();

    protected Conversa() {
    }

    public Conversa(String utilizadorId, String titulo) {
        this.utilizadorId = utilizadorId;
        this.titulo = titulo;
    }

    // Atualiza a data da última atividade
    public void marcarAtualizada() {
        this.atualizadaEm = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getUtilizadorId() { return utilizadorId; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public LocalDateTime getCriadaEm() { return criadaEm; }
    public LocalDateTime getAtualizadaEm() { return atualizadaEm; }
}
