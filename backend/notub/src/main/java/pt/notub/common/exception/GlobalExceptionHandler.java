package pt.notub.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PedidoInvalidoException.class)
    public ResponseEntity<ApiErrorResponse> tratarPedidoInvalido(
            PedidoInvalidoException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> tratarIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), request);
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ApiErrorResponse> tratarArgumentoInvalido(
            Exception ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Bad Request", "Pedido invalido", request);
    }

    @ExceptionHandler(ConflitoException.class)
    public ResponseEntity<ApiErrorResponse> tratarConflito(ConflitoException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "Conflict", ex.getMessage(), request);
    }

    @ExceptionHandler(AutenticacaoRequeridaException.class)
    public ResponseEntity<ApiErrorResponse> tratarAutenticacaoRequerida(
            AutenticacaoRequeridaException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "Authentication Required", ex.getMessage(), request);
    }

    @ExceptionHandler(AcessoNegadoException.class)
    public ResponseEntity<ApiErrorResponse> tratarAcessoNegado(AcessoNegadoException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "Access Denied", ex.getMessage(), request);
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ApiErrorResponse> tratarNaoEncontrado(
            RecursoNaoEncontradoException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> tratarFallback(Exception ex, HttpServletRequest request) {
        ex.printStackTrace();
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "Erro interno", request);
    }

    private ResponseEntity<ApiErrorResponse> build(
            HttpStatus status, String error, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(
                LocalDateTime.now().toString(),
                status.value(),
                error,
                message,
                message,
                request.getRequestURI()
        ));
    }
}
