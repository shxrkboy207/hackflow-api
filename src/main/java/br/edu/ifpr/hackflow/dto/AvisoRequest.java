package br.edu.ifpr.hackflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Corpo de POST /api/avisos. */
public record AvisoRequest(
        @NotBlank(message = "Escreva a mensagem do aviso.")
        @Size(max = 280, message = "O aviso pode ter no máximo 280 caracteres.")
        String texto,

        boolean urgente) {
}
