package br.edu.ifpr.hackflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Corpo de POST /api/auth/login. */
public record LoginRequest(
        @NotBlank(message = "Informe o e-mail.")
        @Size(max = 120, message = "E-mail ou senha incorretos.")
        String email,

        @NotBlank(message = "Informe a senha.")
        @Size(max = 72, message = "E-mail ou senha incorretos.")
        String senha) {

    /** Sem a senha: assim ela nunca aparece por acidente em um log. */
    @Override
    public String toString() {
        return "LoginRequest[email=" + email + "]";
    }
}
