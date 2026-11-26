package br.edu.ifpr.hackflow.controle;

import br.edu.ifpr.hackflow.dto.AvisoRequest;
import br.edu.ifpr.hackflow.dto.AvisoResponse;
import br.edu.ifpr.hackflow.servico.AvisoService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/avisos")
public class AvisoController {

    private final AvisoService servico;

    public AvisoController(AvisoService servico) {
        this.servico = servico;
    }

    /** GET /api/avisos: mural, mais recentes primeiro. */
    @GetMapping
    public List<AvisoResponse> listar() {
        return servico.listar();
    }

    /** POST /api/avisos: publica um aviso (201 | 400). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AvisoResponse publicar(@Valid @RequestBody AvisoRequest corpo) {
        return servico.publicar(corpo);
    }
}
