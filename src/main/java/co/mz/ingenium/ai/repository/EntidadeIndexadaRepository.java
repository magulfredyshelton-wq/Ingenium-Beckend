package co.mz.ingenium.ai.repository;

import co.mz.ingenium.ai.model.EntidadeIndexada;
import co.mz.ingenium.ai.model.TipoEntidade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Necessidade: aceder às entidades indexadas (perfis, vagas, conhecimento) e aos seus conceitos.
 * Funções: procurar por tipo+referência, carregar várias já com os conceitos (evita o problema
 * N+1) e calcular as competências mais pedidas nas vagas ativas.
 */
public interface EntidadeIndexadaRepository extends JpaRepository<EntidadeIndexada, Long> {

    // Procura um registo concreto (ex.: PERFIL do estudante "42")
    Optional<EntidadeIndexada> findByTipoAndReferenciaId(TipoEntidade tipo, String referenciaId);

    // Lista todas as entidades de um tipo (ex.: todo o CONHECIMENTO)
    List<EntidadeIndexada> findByTipoOrderByAtualizadoEmDesc(TipoEntidade tipo);

    // JPQL com "join fetch": traz as entidades E os conceitos numa só consulta
    @Query("select distinct e from EntidadeIndexada e left join fetch e.tags "
            + "where e.tipo = :tipo and e.referenciaId in :ids")
    List<EntidadeIndexada> comTags(@Param("tipo") TipoEntidade tipo, @Param("ids") Collection<String> ids);

    // Nomes das tags que mais aparecem nas vagas ativas
    @Query(value = "select t.name from ai_entidade_tag et "
            + "join ai_entidade_indexada e on e.id = et.entidade_id "
            + "join tag t on t.id = et.tag_id "
            + "where e.tipo = 'VAGA' and e.ativa = true "
            + "group by t.name order by count(*) desc limit :limite", nativeQuery = true)
    List<String> competenciasMaisPedidas(@Param("limite") int limite);
}
