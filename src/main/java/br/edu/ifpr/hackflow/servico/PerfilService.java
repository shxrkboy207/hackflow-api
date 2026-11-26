package br.edu.ifpr.hackflow.servico;

import br.edu.ifpr.hackflow.dto.PerfilResponse;
import br.edu.ifpr.hackflow.dto.PerfilResponse.Desempenho;
import br.edu.ifpr.hackflow.dto.PerfilResponse.EquipePerfil;
import br.edu.ifpr.hackflow.dto.PerfilResponse.ProjetoPerfil;
import br.edu.ifpr.hackflow.dto.ResultadoResponse;
import br.edu.ifpr.hackflow.dto.ResultadoResponse.ItemRanking;
import br.edu.ifpr.hackflow.erro.ApiException;
import br.edu.ifpr.hackflow.repositorio.EquipeRepository;
import br.edu.ifpr.hackflow.repositorio.EquipeRepository.EquipeCompleta;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** Monta o perfil da pessoa logada usando só o que está no banco. */
@Service
public class PerfilService {

    private final EquipeRepository equipes;
    private final ResultadoService resultado;

    public PerfilService(EquipeRepository equipes, ResultadoService resultado) {
        this.equipes = equipes;
        this.resultado = resultado;
    }

    public PerfilResponse montar(long equipeId) {
        EquipeCompleta e = equipes.buscarCompleta(equipeId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Sessão inválida. Faça login novamente."));

        // Nota e posição vêm do mesmo ranking da tela de resultado.
        ResultadoResponse ranking = resultado.montar();
        Double nota = null;
        Integer posicao = null;
        for (ItemRanking item : ranking.ranking()) {
            if (item.id() == equipeId) {
                nota = item.nota();
                posicao = item.posicao();
            }
        }

        ProjetoPerfil projeto = e.enviadoEm() == null
                ? null
                : new ProjetoPerfil(e.repositorio(), e.descricao(), e.enviadoEm());

        return new PerfilResponse(
                e.responsavel(),
                e.email(),
                mascararCelular(e.celular()),
                "RESPONSAVEL", // hoje só o responsável tem conta
                e.criadaEm(),
                new EquipePerfil(e.id(), e.nome(), e.codigo(), e.integrantes()),
                projeto,
                new Desempenho(nota, posicao, ranking.ranking().size()));
    }

    /** 41999990000 vira (41) •••••-0000. O número completo nunca sai do servidor. */
    static String mascararCelular(String digitos) {
        if (digitos == null || digitos.length() < 10) {
            return null;
        }
        String ddd = digitos.substring(0, 2);
        String ultimos = digitos.substring(digitos.length() - 4);
        return "(" + ddd + ") " + "•".repeat(digitos.length() - 6) + "-" + ultimos;
    }
}
