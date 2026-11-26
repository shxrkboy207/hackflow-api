package br.edu.ifpr.hackflow.controle;

import br.edu.ifpr.hackflow.dto.CadastroEquipeRequest;
import br.edu.ifpr.hackflow.dto.EquipeCriadaResponse;
import br.edu.ifpr.hackflow.dto.EquipeResumo;
import br.edu.ifpr.hackflow.dto.NotaRequest;
import br.edu.ifpr.hackflow.dto.NotaResponse;
import br.edu.ifpr.hackflow.dto.ProjetoRequest;
import br.edu.ifpr.hackflow.dto.ProjetoResponse;
import br.edu.ifpr.hackflow.seguranca.CookieSessao;
import br.edu.ifpr.hackflow.servico.AuthService;
import br.edu.ifpr.hackflow.servico.EquipeService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/equipes")
public class EquipeController {

    private final EquipeService servico;
    private final AuthService autenticacao;
    private final CookieSessao cookies;

    public EquipeController(EquipeService servico, AuthService autenticacao, CookieSessao cookies) {
        this.servico = servico;
        this.autenticacao = autenticacao;
        this.cookies = cookies;
    }

    /** POST /api/equipes: inscreve a equipe e já deixa a pessoa logada (201 + cookie | 400 | 409). */
    @PostMapping
    public ResponseEntity<EquipeCriadaResponse> cadastrar(@Valid @RequestBody CadastroEquipeRequest corpo) {
        EquipeCriadaResponse criada = servico.cadastrar(corpo);
        String token = autenticacao.criarSessao(criada.id());
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, cookies.criar(token).toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(criada);
    }

    /** GET /api/equipes: lista as equipes. */
    @GetMapping
    public List<EquipeResumo> listar() {
        // SEGURANÇA (próximo passo): restringir a jurados e comissão.
        return servico.listar();
    }

    /** GET /api/equipes/{id} (200 | 404). */
    @GetMapping("/{id}")
    public EquipeResumo buscar(@PathVariable long id) {
        return servico.buscar(id);
    }

    /** POST /api/equipes/{id}/projeto (201 | 400 | 404). */
    @PostMapping("/{id}/projeto")
    @ResponseStatus(HttpStatus.CREATED)
    public ProjetoResponse enviarProjeto(@PathVariable long id, @Valid @RequestBody ProjetoRequest corpo) {
        // SEGURANÇA (próximo passo): exigir login e conferir se o id é o da equipe logada.
        return servico.enviarProjeto(id, corpo);
    }

    /** PUT /api/equipes/{id}/nota (200 | 400 | 404 | 409). */
    @PutMapping("/{id}/nota")
    public NotaResponse registrarNota(@PathVariable long id, @Valid @RequestBody NotaRequest corpo) {
        // SEGURANÇA (próximo passo): só jurados.
        return servico.registrarNota(id, corpo);
    }
}
