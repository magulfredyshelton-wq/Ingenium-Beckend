package co.mz.ingenium.ai.controller;

import co.mz.ingenium.ai.core.IngeniumPersona;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Necessidade: o backend principal, o Docker e a equipa precisam de saber rapidamente se o
 * módulo está vivo e ligado à base de dados.
 * Funções: responder a GET /api/health com o estado do serviço e da BD (sem autenticação).
 */
@RestController
public class HealthController {

    private final JdbcTemplate jdbc;
    private final IngeniumPersona persona;

    public HealthController(JdbcTemplate jdbc, IngeniumPersona persona) {
        this.jdbc = jdbc;
        this.persona = persona;
    }

    // GET /api/health
    @GetMapping("/api/health")
    public Map<String, Object> saude() {
        // LinkedHashMap mantém a ordem dos campos no JSON
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("estado", "UP");
        r.put("servico", "ingenium-ai");
        r.put("ia", persona.nome());
        try {
            jdbc.queryForObject("SELECT 1", Integer.class);
            r.put("baseDeDados", "UP");
        } catch (RuntimeException e) {
            r.put("baseDeDados", "DOWN");
        }
        return r;
    }
}
