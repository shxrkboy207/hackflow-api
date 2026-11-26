package br.edu.ifpr.hackflow.controle;

import br.edu.ifpr.hackflow.dto.ResultadoResponse;
import br.edu.ifpr.hackflow.servico.ResultadoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/resultado")
public class ResultadoController {

    private final ResultadoService servico;

    public ResultadoController(ResultadoService servico) {
        this.servico = servico;
    }

    /** GET /api/resultado: ranking ordenado + equipes aguardando avaliação. */
    @GetMapping
    public ResultadoResponse resultado() {
        return servico.montar();
    }
}
