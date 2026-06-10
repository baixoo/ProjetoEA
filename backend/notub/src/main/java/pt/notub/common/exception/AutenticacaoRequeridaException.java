package pt.notub.common.exception;

public class AutenticacaoRequeridaException extends RuntimeException {
    public AutenticacaoRequeridaException() {
        super("Autenticacao requerida");
    }
}
