package co.mz.ingenium.ai.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Necessidade: guardar o histórico das recomendações permite medir a qualidade do sistema e
 * alimentar o dashboard (ex.: "que vagas mais recomendámos esta semana?").
 * Funções: representar uma linha de ai_recomendacao.
 */
@Entity
@Table(name = "ai_recomendacao")
public class Recomendacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Estudante que recebeu a recomendação
    @Column(name = "utilizador_id", nullable = false, length = 64)
    private String utilizadorId;

    // "VAGA" ou "PARCEIRO"
    @Column(nullable = false, length = 20)
    private String tipo;

    // Id da vaga ou do estudante recomendado
    @Column(name = "alvo_id", nullable = false, length = 64)
    private String alvoId;

    // Pontuação final (0 a 1)
    @Column(nullable = false)
    private double pontuacao;

    // Explicação gerada pela IA (pode ser nula)
    @Column(columnDefinition = "TEXT")
    private String explicacao;

    @Column(name = "criada_em", nullable = false, updatable = false)
    private LocalDateTime criadaEm = LocalDateTime.now();

    protected Recomendacao() {
    }

    public Recomendacao(String utilizadorId, String tipo, String alvoId, double pontuacao, String explicacao) {
        this.utilizadorId = utilizadorId;
        this.tipo = tipo;
        this.alvoId = alvoId;
        this.pontuacao = pontuacao;
        this.explicacao = explicacao;
    }

    public Long getId() { return id; }
    public String getUtilizadorId() { return utilizadorId; }
    public String getTipo() { return tipo; }
    public String getAlvoId() { return alvoId; }
    public double getPontuacao() { return pontuacao; }
    public String getExplicacao() { return explicacao; }
    public LocalDateTime getCriadaEm() { return criadaEm; }
}
