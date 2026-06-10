package pt.notub.common.exception;

public record ApiErrorResponse(
        String timestamp,
        int status,
        String error,
        String message,
        String mensagem,
        String path
) {
}
