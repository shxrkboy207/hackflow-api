package br.edu.ifpr.hackflow.erro;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Transforma qualquer erro na resposta padrão que o front espera: {"mensagem": "texto"}.
 * Detalhes internos (SQL, stack trace) vão só para o log do servidor, nunca para o cliente.
 */
@RestControllerAdvice
public class ErrosGlobais {

    private static final Logger LOG = LoggerFactory.getLogger(ErrosGlobais.class);

    public record Erro(String mensagem) {
    }

    private static ResponseEntity<Erro> resposta(HttpStatus status, String mensagem) {
        return ResponseEntity.status(status).body(new Erro(mensagem));
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Erro> regraDeNegocio(ApiException e) {
        return resposta(e.getStatus(), e.getMessage());
    }

    /** Falha nas anotações de validação dos DTOs (@NotBlank, @Size...). Devolve a primeira mensagem. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Erro> validacao(MethodArgumentNotValidException e) {
        String mensagem = e.getBindingResult().getFieldErrors().stream()
                .map(erro -> erro.getDefaultMessage())
                .findFirst()
                .orElse("Dados inválidos.");
        return resposta(HttpStatus.BAD_REQUEST, mensagem);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Erro> corpoInvalido(HttpMessageNotReadableException e) {
        return resposta(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido ou com campos desconhecidos.");
    }

    /** Ex.: /api/equipes/abc (id que não é número). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Erro> parametroInvalido(MethodArgumentTypeMismatchException e) {
        return resposta(HttpStatus.NOT_FOUND, "Recurso não encontrado.");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Erro> rotaInexistente(NoResourceFoundException e) {
        return resposta(HttpStatus.NOT_FOUND, "Recurso não encontrado.");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Erro> metodoNaoPermitido(HttpRequestMethodNotSupportedException e) {
        return resposta(HttpStatus.METHOD_NOT_ALLOWED, "Método não permitido para este endereço.");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Erro> tipoNaoSuportado(HttpMediaTypeNotSupportedException e) {
        return resposta(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Envie o corpo como application/json.");
    }

    /** Qualquer outro erro inesperado: registra no log e devolve uma mensagem genérica. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Erro> inesperado(Exception e) {
        // Erros de requisição do próprio Spring (ex.: cabeçalho Accept inválido) mantêm o status 4xx deles.
        if (e instanceof ErrorResponse erroDoSpring && erroDoSpring.getStatusCode().is4xxClientError()) {
            return ResponseEntity.status(erroDoSpring.getStatusCode()).body(new Erro("Requisição inválida."));
        }
        LOG.error("Erro inesperado", e);
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno do servidor. Tente novamente mais tarde.");
    }
}
