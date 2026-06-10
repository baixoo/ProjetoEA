package pt.notub.common.exception;

public class AcessoNegadoException extends RuntimeException {
    public AcessoNegadoException() {
        super("Acesso não autorizado");
    }
}
