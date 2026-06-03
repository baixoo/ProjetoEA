package pt.notub.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> tratarRuntime(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
            "carimboTemporal", LocalDateTime.now().toString(),
            "status", 400,
            "erro", "Pedido Invalido",
            "mensagem", ex.getMessage()
        ));
    }

    @ExceptionHandler(AutenticacaoRequeridaException.class)
    public ResponseEntity<Map<String, Object>> tratarAutenticacaoRequerida(AutenticacaoRequeridaException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
            "carimboTemporal", LocalDateTime.now().toString(),
            "status", 401,
            "erro", "Autenticacao Requerida",
            "mensagem", ex.getMessage()
        ));
    }

    @ExceptionHandler(AcessoNegadoException.class)
    public ResponseEntity<Map<String, Object>> tratarAcessoNegado(AcessoNegadoException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
            "carimboTemporal", LocalDateTime.now().toString(),
            "status", 403,
            "erro", "Acesso Negado",
            "mensagem", ex.getMessage()
        ));
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> tratarNaoEncontrado(RecursoNaoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
            "carimboTemporal", LocalDateTime.now().toString(),
            "status", 404,
            "erro", "Nao Encontrado",
            "mensagem", ex.getMessage()
        ));
    }
}
