package br.edu.ifpr.hackflow.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Corpo de POST /api/equipes. */
public record CadastroEquipeRequest(
        @NotBlank(message = "Informe o nome da equipe.")
        @Size(min = 3, max = 60, message = "O nome da equipe deve ter de 3 a 60 caracteres.")
        String nome,

        @NotBlank(message = "Informe o nome do responsável.")
        @Size(min = 3, max = 80, message = "O nome do responsável deve ter de 3 a 80 caracteres.")
        String responsavel,

        @NotBlank(message = "Informe o e-mail do responsável.")
        @Size(max = 120, message = "O e-mail pode ter no máximo 120 caracteres.")
        @Pattern(regexp = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$", message = "Informe um e-mail válido.")
        String email,

        @NotBlank(message = "Informe uma senha.")
        @Size(min = 8, max = 72, message = "A senha deve ter de 8 a 72 caracteres.")
        String senha,

        @Size(max = 20, message = "O celular pode ter no máximo 20 caracteres.")
        String celular,

        @NotNull(message = "Informe o número de integrantes.")
        @Min(value = 3, message = "A equipe deve ter de 3 a 5 integrantes.")
        @Max(value = 5, message = "A equipe deve ter de 3 a 5 integrantes.")
        Integer integrantes) {

    /** Sem a senha: assim ela nunca aparece por acidente em um log. */
    @Override
    public String toString() {
        return "CadastroEquipeRequest[nome=" + nome + ", email=" + email + "]";
    }
}
