package co.mz.ingenium.ai.repository;

import co.mz.ingenium.ai.model.PedidoLog;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Necessidade: gravar o registo de cada chamada ao Gemini.
 * Funções: operações CRUD herdadas do JpaRepository (save, findAll, count, ...).
 */
public interface PedidoLogRepository extends JpaRepository<PedidoLog, Long> {
}
