package br.edu.ifpr.hackflow.erro;

import org.springframework.http.HttpStatus;

/** Erro previsto pela regra de negócio. Vira {"mensagem": "..."} com o status indicado. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String mensagem) {
        super(mensagem);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
