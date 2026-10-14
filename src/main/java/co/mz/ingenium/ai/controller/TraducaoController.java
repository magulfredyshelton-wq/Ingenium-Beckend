package co.mz.ingenium.ai.controller;

import jakarta.validation.Valid;
import co.mz.ingenium.ai.dto.TraducaoDtos.TraducaoResposta;
import co.mz.ingenium.ai.dto.TraducaoDtos.TraduzirPedido;
import co.mz.ingenium.ai.seguranca.AutenticacaoFilter;
import co.mz.ingenium.ai.seguranca.UtilizadorAutenticado;
import co.mz.ingenium.ai.service.TranslationService;
import org.springframework.web.bind.annotation.*;

/**
 * Necessidade: expor a tradução (botão "Traduzir" em vagas, perfis e posts).
 * Funções: POST /api/ai/traducao.
 */
@RestController
@RequestMapping("/api/ai/traducao")
public class TraducaoController {

    private final TranslationService servico;

    public TraducaoController(TranslationService servico) {
        this.servico = servico;
    }

    // Traduz o texto
    @PostMapping
    public TraducaoResposta traduzir(@RequestAttribute(AutenticacaoFilter.ATRIBUTO) UtilizadorAutenticado eu,
                                     @Valid @RequestBody TraduzirPedido pedido) {
        return servico.traduzir(eu.id(), pedido.texto(), pedido.idiomaDestino());
    }
}
