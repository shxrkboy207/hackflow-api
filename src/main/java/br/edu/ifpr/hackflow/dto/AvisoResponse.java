package br.edu.ifpr.hackflow.dto;

public record AvisoResponse(long id, String texto, boolean urgente, String criadoEm) {
}
