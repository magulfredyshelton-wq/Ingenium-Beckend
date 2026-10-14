package co.mz.ingenium.ai.repository;

import co.mz.ingenium.ai.model.Conversa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Necessidade: aceder à tabela ai_conversa sem escrever SQL.
 * Funções: listar as conversas de um estudante e obter uma conversa garantindo que é dele.
 */
public interface ConversaRepository extends JpaRepository<Conversa, Long> {

    // SELECT ... WHERE utilizador_id = ? ORDER BY atualizada_em DESC (mais recentes primeiro)
    List<Conversa> findByUtilizadorIdOrderByAtualizadaEmDesc(String utilizadorId);

    // SELECT ... WHERE id = ? AND utilizador_id = ? (isolamento: nunca devolve conversas de outro)
    Optional<Conversa> findByIdAndUtilizadorId(Long id, String utilizadorId);
}
