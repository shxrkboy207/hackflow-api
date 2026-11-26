package br.edu.ifpr.hackflow.seguranca;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Cookie que guarda o token de login.
 *  - HttpOnly: o JavaScript da página não consegue ler (protege o token contra XSS);
 *  - SameSite=Strict: o navegador não envia o cookie em requisições vindas de outros sites;
 *  - Secure: só viaja por HTTPS (ligue em hackflow.cookie.seguro=true quando houver HTTPS).
 */
@Component
public class CookieSessao {

    public static final String NOME = "hf_sessao";

    private final boolean seguro;
    private final long horas;

    public CookieSessao(@Value("${hackflow.cookie.seguro:false}") boolean seguro,
                        @Value("${hackflow.sessao.horas:12}") long horas) {
        this.seguro = seguro;
        this.horas = horas;
    }

    public ResponseCookie criar(String token) {
        return base(token).maxAge(Duration.ofHours(horas)).build();
    }

    /** Cookie vazio e já vencido: manda o navegador apagar o login. */
    public ResponseCookie apagar() {
        return base("").maxAge(Duration.ZERO).build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String valor) {
        return ResponseCookie.from(NOME, valor)
                .httpOnly(true)
                .secure(seguro)
                .sameSite("Strict")
                .path("/");
    }

    /** Lê o token do cookie da requisição (null se não houver). */
    public static String ler(HttpServletRequest requisicao) {
        Cookie[] cookies = requisicao.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (NOME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
