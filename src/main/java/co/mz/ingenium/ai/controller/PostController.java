package co.mz.ingenium.ai.controller;

import jakarta.validation.Valid;
import co.mz.ingenium.ai.dto.PostDtos.AnalisarPostPedido;
import co.mz.ingenium.ai.dto.PostDtos.AnalisePost;
import co.mz.ingenium.ai.seguranca.AutenticacaoFilter;
import co.mz.ingenium.ai.seguranca.UtilizadorAutenticado;
import co.mz.ingenium.ai.service.PostAnalysisService;
import org.springframework.web.bind.annotation.*;

/**
 * Necessidade: o módulo de feed chama este endpoint antes de publicar (moderação + hashtags).
 * Funções: POST /api/ai/posts/analisar.
 */
@RestController
@RequestMapping("/api/ai/posts")
public class PostController {

    private final PostAnalysisService servico;

    public PostController(PostAnalysisService servico) {
        this.servico = servico;
    }

    // @Valid aplica @NotBlank/@Size ao corpo
    @PostMapping("/analisar")
    public AnalisePost analisar(@RequestAttribute(AutenticacaoFilter.ATRIBUTO) UtilizadorAutenticado eu,
                                @Valid @RequestBody AnalisarPostPedido pedido) {
        // O id pode vir vazio quando é o backend a chamar
        return servico.analisar(eu.id(), pedido.texto());
    }
}
