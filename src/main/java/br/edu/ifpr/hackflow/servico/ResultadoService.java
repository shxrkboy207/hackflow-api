package br.edu.ifpr.hackflow.servico;

import br.edu.ifpr.hackflow.dto.EquipeResumo;
import br.edu.ifpr.hackflow.dto.ResultadoResponse;
import br.edu.ifpr.hackflow.dto.ResultadoResponse.ItemAguardando;
import br.edu.ifpr.hackflow.dto.ResultadoResponse.ItemRanking;
import br.edu.ifpr.hackflow.repositorio.EquipeRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ResultadoService {

    private final EquipeRepository repositorio;

    public ResultadoService(EquipeRepository repositorio) {
        this.repositorio = repositorio;
    }

    public ResultadoResponse montar() {
        List<EquipeResumo> todas = repositorio.listar();

        // Equipes com nota: da maior para a menor (empate de nota: ordem alfabética).
        List<EquipeResumo> avaliadas = todas.stream()
                .filter(e -> e.nota() != null)
                .sorted(Comparator.comparing(EquipeResumo::nota, Comparator.reverseOrder())
                        .thenComparing(EquipeResumo::nome))
                .toList();

        // Notas iguais dividem a mesma posição (1, 2, 2, 4...).
        List<ItemRanking> ranking = new ArrayList<>();
        for (int i = 0; i < avaliadas.size(); i++) {
            EquipeResumo equipe = avaliadas.get(i);
            boolean empatouComAnterior = i > 0 && equipe.nota().equals(avaliadas.get(i - 1).nota());
            int posicao = empatouComAnterior ? ranking.get(i - 1).posicao() : i + 1;
            ranking.add(new ItemRanking(posicao, equipe.id(), equipe.nome(), equipe.nota()));
        }

        // Com projeto enviado e ainda sem nota.
        List<ItemAguardando> aguardando = todas.stream()
                .filter(e -> e.temProjeto() && e.nota() == null)
                .map(e -> new ItemAguardando(e.id(), e.nome()))
                .toList();

        return new ResultadoResponse(ranking, aguardando);
    }
}
