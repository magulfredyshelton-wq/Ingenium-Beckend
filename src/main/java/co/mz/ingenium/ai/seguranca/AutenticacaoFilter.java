package co.mz.ingenium.ai.seguranca;

import co.mz.ingenium.entity.UserEntity.StudentEntity.Students;
import co.mz.ingenium.entity.UserEntity.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Liga o Spring Security à IA: o JwtAuthFilter do backend já pôs o User no contexto;
 * aqui só o convertemos em UtilizadorAutenticado e guardamo-lo no pedido.
 * Quem não estiver autenticado nunca chega aqui: o SecurityConfig recusa antes.
 */
@Component
public class AutenticacaoFilter extends OncePerRequestFilter {

    /** Nome do atributo usado pelos controllers com @RequestAttribute. */
    public static final String ATRIBUTO = "ingenium.utilizador";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest pedido) {
        return !pedido.getRequestURI().startsWith("/api/ai/") || "OPTIONS".equalsIgnoreCase(pedido.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest pedido, HttpServletResponse resposta, FilterChain cadeia)
            throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            String papel = (user instanceof Students)
                    ? UtilizadorAutenticado.ESTUDANTE
                    : UtilizadorAutenticado.EMPRESA;
            pedido.setAttribute(ATRIBUTO,
                    new UtilizadorAutenticado(user.getId().toString(), user.getDisplayName(), papel));
        }
        cadeia.doFilter(pedido, resposta);
    }
}