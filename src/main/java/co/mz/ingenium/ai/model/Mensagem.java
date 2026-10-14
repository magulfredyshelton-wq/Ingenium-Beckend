package co.mz.ingenium.ai.model;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

/**
 * Necessidade: guardar o histórico do chat, que serve de "memória" ao Ingenium e é mostrado
 * quando o estudante reabre uma conversa.
 * Funções: representar uma linha da tabela ai_mensagem.
 */
@Entity
@Table(name = "ai_mensagem")
public class Mensagem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Muitas mensagens pertencem a UMA conversa; LAZY = só carrega a conversa se for pedida
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    // Coluna da chave estrangeira
    @JoinColumn(name = "conversa_id", nullable = false)
    // Gera "ON DELETE CASCADE": apagar a conversa apaga as suas mensagens na base de dados
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Conversa conversa;

    // Grava o enum como texto ("UTILIZADOR"/"ASSISTENTE") e não como número
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private PapelMensagem papel;

    // Texto da mensagem; TEXT não tem limite prático
    @Column(nullable = false, columnDefinition = "TEXT")
    private String conteudo;

    // Data de criação
    @Column(name = "criada_em", nullable = false, updatable = false)
    private LocalDateTime criadaEm = LocalDateTime.now();

    protected Mensagem() {
    }

    public Mensagem(Conversa conversa, PapelMensagem papel, String conteudo) {
        this.conversa = conversa;
        this.papel = papel;
        this.conteudo = conteudo;
    }

    public Long getId() { return id; }
    public Conversa getConversa() { return conversa; }
    public PapelMensagem getPapel() { return papel; }
    public String getConteudo() { return conteudo; }
    public LocalDateTime getCriadaEm() { return criadaEm; }
}
