package br.edu.ifpr.hackflow.controle;

import br.edu.ifpr.hackflow.dto.LoginRequest;
import br.edu.ifpr.hackflow.dto.SessaoResponse;
import br.edu.ifpr.hackflow.seguranca.CookieSessao;
import br.edu.ifpr.hackflow.servico.AuthService;
import br.edu.ifpr.hackflow.servico.AuthService.ResultadoLogin;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService autenticacao;
    private final CookieSessao cookies;

    public AuthController(AuthService autenticacao, CookieSessao cookies) {
        this.autenticacao = autenticacao;
        this.cookies = cookies;
    }

    /** POST /api/auth/login (200 + cookie | 401 | 429). */
    @PostMapping("/login")
    public ResponseEntity<SessaoResponse> login(@Valid @RequestBody LoginRequest corpo) {
        ResultadoLogin resultado = autenticacao.login(corpo);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookies.criar(resultado.token()).toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(resultado.sessao());
    }

    /** POST /api/auth/logout: encerra a sessão e apaga o cookie (204). */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest requisicao) {
        autenticacao.encerrarSessao(CookieSessao.ler(requisicao));
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookies.apagar().toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .build();
    }

    /** GET /api/auth/sessao: 200 com quem está logado, ou 204 se ninguém está (o front usa ao abrir o site). */
    @GetMapping("/sessao")
    public ResponseEntity<SessaoResponse> sessao(HttpServletRequest requisicao) {
        Optional<SessaoResponse> sessao = autenticacao
                .equipeDoToken(CookieSessao.ler(requisicao))
                .flatMap(autenticacao::sessaoDe);
        if (sessao.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-store").body(sessao.get());
    }
}
