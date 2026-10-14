package co.mz.ingenium.ai.service;

import co.mz.ingenium.ai.core.AiPromptExecutor;
import co.mz.ingenium.ai.core.AiRequestContext;
import co.mz.ingenium.ai.core.AiResponseValidator;
import co.mz.ingenium.ai.core.IngeniumPersona;
import co.mz.ingenium.ai.dto.PerfilDtos.SugestoesPerfil;
import co.mz.ingenium.ai.exception.RecursoNaoEncontradoException;
import co.mz.ingenium.ai.model.EntidadeIndexada;
import co.mz.ingenium.ai.model.Tag;
import co.mz.ingenium.ai.model.TipoEntidade;
import co.mz.ingenium.ai.repository.EntidadeIndexadaRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static co.mz.ingenium.ai.core.AiConstants.SERVICO_PERFIL;

/** Analisa o perfil do estudante e sugere melhorias e tags novas a aprender. */
@Service
public class ProfileImprovementService {

    private final EntidadeIndexadaRepository entidades;
    private final TagService tagService;
    private final AiPromptExecutor executor;
    private final AiResponseValidator validador;
    private final IngeniumPersona persona;

    public ProfileImprovementService(EntidadeIndexadaRepository entidades, TagService tagService,
                                     AiPromptExecutor executor, AiResponseValidator validador, IngeniumPersona persona) {
        this.entidades = entidades;
        this.tagService = tagService;
        this.executor = executor;
        this.validador = validador;
        this.persona = persona;
    }

    public SugestoesPerfil sugerir(String utilizadorId, String objetivo) {
        EntidadeIndexada perfil = entidades.comTags(TipoEntidade.PERFIL, List.of(utilizadorId)).stream().findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException("O teu perfil ainda não foi enviado ao Ingenium."));
        List<String> procuradas = tagService.competenciasMaisPedidas(15);
        Set<UUID> minhas = perfil.getTags().stream().map(Tag::getId).collect(Collectors.toSet());
        String sistema = persona.identidade() + """

                TAREFA: avaliar o perfil profissional de um estudante e sugerir melhorias.
                CRITÉRIOS: clareza do "Sobre", competências concretas, projetos/portfólio, certificados, \
                experiências (inclui voluntariado e projetos escolares) e alinhamento com o objetivo.
                REGRAS:
                - "pontuacao" de 0 a 100.
                - No máximo 5 itens em "pontosFortes" e em "melhorias"; cada item com uma ação concreta.
                - "competenciasSugeridas": até 6, EXCLUSIVAMENTE da lista de tags abaixo (copiadas exatamente), \
                de preferência as mais procuradas nas vagas.
                - "sobreSugerido": até 600 caracteres, na 1.ª pessoa, só com factos do perfil.
                - Nunca inventes experiências ou certificados.

                TAGS DISPONÍVEIS (por área):
                """ + tagService.catalogoParaPrompt();
        String dados = "PERFIL:\n" + perfil.getTexto()
                + "\nTAGS DO ESTUDANTE: " + perfil.getTags().stream().map(Tag::getName).toList()
                + "\nOBJETIVO DO ESTUDANTE: " + (objetivo == null || objetivo.isBlank() ? "(não indicado)" : objetivo)
                + "\nTAGS MAIS PROCURADAS NAS VAGAS DO INGENIUM: " + (procuradas.isEmpty() ? "(ainda sem vagas)" : procuradas);
        SugestoesPerfil s = executor.executarEstruturado(AiRequestContext.iniciar(utilizadorId, SERVICO_PERFIL),
                sistema, dados, SugestoesPerfil.class);
        // Só tags reais da base de dados, e que o estudante ainda não tem
        List<String> sugeridas = validador.limparLista(s.competenciasSugeridas(), 6).stream()
                .map(tagService::resolver)
                .flatMap(Optional::stream)
                .filter(t -> !minhas.contains(t.getId()))
                .map(Tag::getName)
                .distinct()
                .toList();
        return new SugestoesPerfil(validador.limitar(s.pontuacao(), 0, 100), s.resumo().strip(),
                validador.limparLista(s.pontosFortes(), 5), validador.limparLista(s.melhorias(), 5),
                sugeridas, s.sobreSugerido() == null ? null : s.sobreSugerido().strip());
    }
}