package br.edu.ifpr.hackflow.config;

import br.edu.ifpr.hackflow.seguranca.AutenticacaoInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebSegurancaConfig implements WebMvcConfigurer {

    private final AutenticacaoInterceptor autenticacao;

    public WebSegurancaConfig(AutenticacaoInterceptor autenticacao) {
        this.autenticacao = autenticacao;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registro) {
        // SEGURANÇA (próximo passo): acrescentar aqui as rotas de escrita (projeto, nota, avisos).
        registro.addInterceptor(autenticacao).addPathPatterns("/api/perfil", "/api/perfil/**");
    }
}
