package co.mz.ingenium.ai.controller;

import co.mz.ingenium.ai.dto.RecomendacaoDtos.*;
import co.mz.ingenium.ai.seguranca.AutenticacaoFilter;
import co.mz.ingenium.ai.seguranca.UtilizadorAutenticado;
import co.mz.ingenium.ai.service.JobRecommendationService;
import co.mz.ingenium.ai.service.PartnerMatchingService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Necessidade: expor as recomendações inteligentes (vagas e parceiros) ao frontend/backend.
 * Funções: GET de vagas recomendadas, parceiros recomendados e histórico.
 */
@RestController
@RequestMapping("/api/ai/recomendacoes")
public class RecomendacaoController {

    private final JobRecommendationService vagas;
    private final PartnerMatchingService parceiros;

    public RecomendacaoController(JobRecommendationService vagas, PartnerMatchingService parceiros) {
        this.vagas = vagas;
        this.parceiros = parceiros;
    }

    // GET /api/ai/recomendacoes/vagas?limite=5&explicar=true
    @GetMapping("/vagas")
    public List<VagaRecomendada> vagas(@RequestAttribute(AutenticacaoFilter.ATRIBUTO) UtilizadorAutenticado eu,
                                       @RequestParam(defaultValue = "5") int limite,
                                       @RequestParam(defaultValue = "true") boolean explicar) {
        return vagas.recomendar(eu.exigirId(), limite, explicar);
    }

    // GET /api/ai/recomendacoes/parceiros?objetivo=...&limite=5&explicar=true
    @GetMapping("/parceiros")
    public List<ParceiroRecomendado> parceiros(@RequestAttribute(AutenticacaoFilter.ATRIBUTO) UtilizadorAutenticado eu,
                                               @RequestParam(required = false) String objetivo,
                                               @RequestParam(defaultValue = "5") int limite,
                                               @RequestParam(defaultValue = "true") boolean explicar) {
        return parceiros.recomendar(eu.exigirId(), objetivo, limite, explicar);
    }

    // GET /api/ai/recomendacoes/historico
    @GetMapping("/historico")
    public List<HistoricoItem> historico(@RequestAttribute(AutenticacaoFilter.ATRIBUTO) UtilizadorAutenticado eu) {
        return vagas.historico(eu.exigirId());
    }
}
