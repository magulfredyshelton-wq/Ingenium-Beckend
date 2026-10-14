package co.mz.ingenium.ai.controller;

import jakarta.validation.Valid;
import co.mz.ingenium.ai.dto.PerfilDtos.PedidoSugestoes;
import co.mz.ingenium.ai.dto.PerfilDtos.SugestoesPerfil;
import co.mz.ingenium.ai.seguranca.AutenticacaoFilter;
import co.mz.ingenium.ai.seguranca.UtilizadorAutenticado;
import co.mz.ingenium.ai.service.ProfileImprovementService;
import org.springframework.web.bind.annotation.*;

/**
 * Necessidade: expor a análise de perfil ao frontend (botão "Melhorar o meu perfil com o Ingenium").
 * Funções: POST /api/ai/perfil/sugestoes.
 */
@RestController
@RequestMapping("/api/ai/perfil")
public class PerfilController {

    private final ProfileImprovementService servico;

    public PerfilController(ProfileImprovementService servico) {
        this.servico = servico;
    }

    // O corpo é opcional (required = false)
    @PostMapping("/sugestoes")
    public SugestoesPerfil sugestoes(@RequestAttribute(AutenticacaoFilter.ATRIBUTO) UtilizadorAutenticado eu,
                                     @Valid @RequestBody(required = false) PedidoSugestoes pedido) {
        return servico.sugerir(eu.exigirId(), pedido == null ? null : pedido.objetivo());
    }
}
