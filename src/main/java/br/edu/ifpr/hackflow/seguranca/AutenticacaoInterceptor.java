package br.edu.ifpr.hackflow.seguranca;

import br.edu.ifpr.hackflow.erro.ApiException;
import br.edu.ifpr.hackflow.servico.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Roda antes das rotas protegidas: confere o cookie de login. Sem sessão válida responde 401;
 * com sessão válida guarda o id da equipe logada na requisição para o controlador usar.
 */
@Component
public class AutenticacaoInterceptor implements HandlerInterceptor {

    /** Nome do atributo da requisição que carrega o id da equipe logada. */
    public static final String ATRIBUTO = "equipeAutenticadaId";

    private final AuthService autenticacao;

    public AutenticacaoInterceptor(AuthService autenticacao) {
        this.autenticacao = autenticacao;
    }

    @Override
    public boolean preHandle(HttpServletRequest requisicao, HttpServletResponse resposta, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(requisicao.getMethod())) {
            return true; // pré-verificação de CORS não leva cookie
        }
        // Dados do usuário nunca devem ficar guardados em cache do navegador ou de intermediários.
        resposta.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");

        Long equipeId = autenticacao.equipeDoToken(CookieSessao.ler(requisicao)).orElse(null);
        if (equipeId == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Faça login para continuar.");
        }
        requisicao.setAttribute(ATRIBUTO, equipeId);
        return true;
    }
}
