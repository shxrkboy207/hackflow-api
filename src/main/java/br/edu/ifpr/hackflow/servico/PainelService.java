package br.edu.ifpr.hackflow.servico;

import br.edu.ifpr.hackflow.dto.PainelResponse;
import br.edu.ifpr.hackflow.dto.PainelResponse.Situacao;
import br.edu.ifpr.hackflow.repositorio.PainelRepository;
import br.edu.ifpr.hackflow.util.Tempo;
import org.springframework.stereotype.Service;

/** Números do painel: tudo vem de consultas ao banco, nada é valor fixo. */
@Service
public class PainelService {

    private final PainelRepository repositorio;

    public PainelService(PainelRepository repositorio) {
        this.repositorio = repositorio;
    }

    public PainelResponse resumo() {
        int equipes = repositorio.contarEquipes();
        int projetos = repositorio.contarProjetos();
        int avaliadas = repositorio.contarAvaliadas();
        int aguardando = repositorio.contarAguardando();

        return new PainelResponse(
                Tempo.agora(),
                equipes,
                projetos,
                avaliadas,
                aguardando,
                repositorio.notasPorEquipe(),
                new Situacao(avaliadas, aguardando, Math.max(0, equipes - projetos)),
                repositorio.inscricoesPorDia());
    }
}
