package pt.notub.exception;

public class AutenticacaoRequeridaException extends RuntimeException {
    public AutenticacaoRequeridaException() {
        super("Autenticacao requerida");
    }
}
