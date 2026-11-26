package br.edu.ifpr.hackflow.dto;

import java.util.List;

/** Resposta de GET /api/resultado: ranking já ordenado e equipes que aguardam avaliação. */
public record ResultadoResponse(List<ItemRanking> ranking, List<ItemAguardando> aguardando) {

    public record ItemRanking(int posicao, long id, String nome, double nota) {
    }

    public record ItemAguardando(long id, String nome) {
    }
}
