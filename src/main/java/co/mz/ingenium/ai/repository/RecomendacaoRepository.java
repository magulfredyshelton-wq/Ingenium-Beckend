package co.mz.ingenium.ai.repository;

import co.mz.ingenium.ai.model.Recomendacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Necessidade: gravar e consultar o histórico de recomendações.
 * Funções: operações CRUD herdadas + últimas recomendações de um estudante.
 */
public interface RecomendacaoRepository extends JpaRepository<Recomendacao, Long> {

    // As 20 recomendações mais recentes de um estudante
    List<Recomendacao> findTop20ByUtilizadorIdOrderByCriadaEmDesc(String utilizadorId);
}
