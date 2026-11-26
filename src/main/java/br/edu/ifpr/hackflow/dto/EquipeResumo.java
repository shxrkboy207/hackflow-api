package br.edu.ifpr.hackflow.dto;

/** Equipe nas listas e na ficha de avaliação. nota = null enquanto não avaliada. */
public record EquipeResumo(long id, String nome, boolean temProjeto, Double nota) {
}
