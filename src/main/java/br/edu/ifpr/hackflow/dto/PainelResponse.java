package br.edu.ifpr.hackflow.dto;

import java.util.List;

/** Resposta de GET /api/painel/resumo: tudo calculado a partir do banco. */
public record PainelResponse(
        String atualizadoEm,
        int equipes,
        int projetosEnviados,
        int equipesAvaliadas,
        int aguardandoAvaliacao,
        List<BarraEquipe> notasPorEquipe,
        Situacao situacao,
        List<PontoDia> inscricoesPorDia) {

    /** Nota final de uma equipe avaliada. */
    public record BarraEquipe(String nome, double valor) {
    }

    public record Situacao(int avaliadas, int aguardando, int semProjeto) {
    }

    /** Quantas equipes se inscreveram em um dia (dia no formato dd/MM). */
    public record PontoDia(String dia, int total) {
    }
}
