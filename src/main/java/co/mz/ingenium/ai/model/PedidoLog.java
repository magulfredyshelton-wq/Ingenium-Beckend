package co.mz.ingenium.ai.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Necessidade: o Gemini tem limites de uso e pode falhar. Precisamos de saber quantas chamadas
 * cada serviço faz, quanto tempo demoram e quantas falham.
 * Funções: representar uma linha de ai_pedido_log (auditoria/monitorização das chamadas à IA).
 */
@Entity
@Table(name = "ai_pedido_log")
public class PedidoLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Quem originou a chamada (pode ser nulo)
    @Column(name = "utilizador_id", length = 64)
    private String utilizadorId;

    // Nome do serviço (CAREER_CHAT, JOB_RECOMMENDATION, ...)
    @Column(nullable = false, length = 40)
    private String servico;

    // Duração em milissegundos
    @Column(name = "duracao_ms", nullable = false)
    private long duracaoMs;

    // A chamada correu bem?
    @Column(nullable = false)
    private boolean sucesso;

    // Mensagem de erro (cortada a 500 caracteres)
    @Column(length = 500)
    private String erro;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    protected PedidoLog() {
    }

    public PedidoLog(String utilizadorId, String servico, long duracaoMs, boolean sucesso, String erro) {
        this.utilizadorId = utilizadorId;
        this.servico = servico;
        this.duracaoMs = duracaoMs;
        this.sucesso = sucesso;
        this.erro = erro == null ? null : erro.substring(0, Math.min(erro.length(), 500));
    }

    public Long getId() { return id; }
    public String getServico() { return servico; }
    public long getDuracaoMs() { return duracaoMs; }
    public boolean isSucesso() { return sucesso; }
}
