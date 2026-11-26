package br.edu.ifpr.hackflow.controle;

import br.edu.ifpr.hackflow.dto.PainelResponse;
import br.edu.ifpr.hackflow.servico.PainelService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/painel")
public class PainelController {

    private final PainelService servico;

    public PainelController(PainelService servico) {
        this.servico = servico;
    }

    /** GET /api/painel/resumo: números e gráficos do painel. */
    @GetMapping("/resumo")
    public PainelResponse resumo() {
        return servico.resumo();
    }
}
