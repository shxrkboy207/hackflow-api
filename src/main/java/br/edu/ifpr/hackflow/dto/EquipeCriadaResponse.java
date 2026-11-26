package br.edu.ifpr.hackflow.dto;

/** Resposta 201 de POST /api/equipes. */
public record EquipeCriadaResponse(long id, String nome, String codigo) {
}
