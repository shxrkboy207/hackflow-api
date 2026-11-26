package br.edu.ifpr.hackflow.dto;

/** Quem está logado: nome da pessoa responsável, nome da equipe e id da equipe. */
public record SessaoResponse(String nome, String equipe, long equipeId) {
}
