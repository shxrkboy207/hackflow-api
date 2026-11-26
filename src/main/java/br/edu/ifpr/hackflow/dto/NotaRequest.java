package br.edu.ifpr.hackflow.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/** Corpo de PUT /api/equipes/{id}/nota. Cada critério vai de 0 a 10. */
public record NotaRequest(
        @NotNull(message = "Informe a nota de inovação.")
        @DecimalMin(value = "0.0", message = "As notas devem estar entre 0 e 10.")
        @DecimalMax(value = "10.0", message = "As notas devem estar entre 0 e 10.")
        Double inovacao,

        @NotNull(message = "Informe a nota de execução técnica.")
        @DecimalMin(value = "0.0", message = "As notas devem estar entre 0 e 10.")
        @DecimalMax(value = "10.0", message = "As notas devem estar entre 0 e 10.")
        Double execucaoTecnica,

        @NotNull(message = "Informe a nota de apresentação.")
        @DecimalMin(value = "0.0", message = "As notas devem estar entre 0 e 10.")
        @DecimalMax(value = "10.0", message = "As notas devem estar entre 0 e 10.")
        Double apresentacao) {
}
