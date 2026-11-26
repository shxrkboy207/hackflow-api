package br.edu.ifpr.hackflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Corpo de POST /api/equipes/{id}/projeto. */
public record ProjetoRequest(
        @NotBlank(message = "Informe o link do repositório.")
        @Size(max = 300, message = "O link do repositório pode ter no máximo 300 caracteres.")
        String repositorio,

        @Size(max = 500, message = "A descrição pode ter no máximo 500 caracteres.")
        String descricao) {
}
