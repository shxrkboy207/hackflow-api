package br.edu.ifpr.hackflow.dto;

/** Resposta de GET /api/perfil: só dados que existem no banco. */
public record PerfilResponse(
        String nome,
        String email,
        String celularMascarado,
        String papel,
        String inscritoEm,
        EquipePerfil equipe,
        ProjetoPerfil projeto,
        Desempenho desempenho) {

    public record EquipePerfil(long id, String nome, String codigo, int integrantes) {
    }

    /** null no PerfilResponse quando a equipe ainda não enviou o projeto. */
    public record ProjetoPerfil(String repositorio, String descricao, String enviadoEm) {
    }

    /** nota e posicao são null enquanto a equipe não foi avaliada. */
    public record Desempenho(Double nota, Integer posicao, int totalAvaliadas) {
    }
}
