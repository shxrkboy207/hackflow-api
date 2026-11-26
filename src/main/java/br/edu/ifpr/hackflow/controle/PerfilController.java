package br.edu.ifpr.hackflow.controle;

import br.edu.ifpr.hackflow.dto.PerfilResponse;
import br.edu.ifpr.hackflow.seguranca.AutenticacaoInterceptor;
import br.edu.ifpr.hackflow.servico.PerfilService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/perfil")
public class PerfilController {

    private final PerfilService servico;

    public PerfilController(PerfilService servico) {
        this.servico = servico;
    }

    /**
     * GET /api/perfil: dados de quem está logado. O id vem da SESSÃO (colocado pelo interceptor),
     * nunca da URL, então ninguém consegue ver o perfil de outra equipe.
     */
    @GetMapping
    public PerfilResponse perfil(@RequestAttribute(AutenticacaoInterceptor.ATRIBUTO) long equipeId) {
        return servico.montar(equipeId);
    }
}
