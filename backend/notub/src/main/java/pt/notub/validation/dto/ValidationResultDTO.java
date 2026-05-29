package pt.notub.validation.dto;

public class ValidationResultDTO {

    private boolean valido;
    private String mensagem;
    private boolean consumido;

    public ValidationResultDTO() {}

    public ValidationResultDTO(boolean valido, String mensagem, boolean consumido) {
        this.valido = valido;
        this.mensagem = mensagem;
        this.consumido = consumido;
    }

    public boolean isValido() { return valido; }
    public void setValido(boolean valido) { this.valido = valido; }
    public String getMensagem() { return mensagem; }
    public void setMensagem(String mensagem) { this.mensagem = mensagem; }
    public boolean isConsumido() { return consumido; }
    public void setConsumido(boolean consumido) { this.consumido = consumido; }
}
