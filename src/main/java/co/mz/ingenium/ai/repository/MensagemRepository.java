package co.mz.ingenium.ai.repository;

import co.mz.ingenium.ai.model.Mensagem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Necessidade: ler e gravar as mensagens do chat.
 * Funções: devolver o histórico completo (para o ecrã) e as últimas N (para a memória da IA).
 */
public interface MensagemRepository extends JpaRepository<Mensagem, Long> {

    // Histórico completo, da mais antiga para a mais recente
    List<Mensagem> findByConversaIdOrderByIdAsc(Long conversaId);

    // As mais recentes primeiro; o Pageable limita a quantidade (ex.: 10)
    List<Mensagem> findByConversaIdOrderByIdDesc(Long conversaId, Pageable pagina);
}
