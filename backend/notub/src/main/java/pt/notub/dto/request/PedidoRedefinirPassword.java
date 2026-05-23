package pt.notub.dto.request;

public class PedidoRedefinirPassword {
    private String token;
    private String novaPassword;

    public PedidoRedefinirPassword() {}

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getNovaPassword() { return novaPassword; }
    public void setNovaPassword(String novaPassword) { this.novaPassword = novaPassword; }
}
