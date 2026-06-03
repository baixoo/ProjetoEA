package pt.notub.payment.dto;

public class CheckoutResponse {
    private Long transacaoId;
    private String token;
    private String redirectUrl;
    private String estado;

    public CheckoutResponse() {}

    public CheckoutResponse(Long transacaoId, String token, String redirectUrl, String estado) {
        this.transacaoId = transacaoId;
        this.token = token;
        this.redirectUrl = redirectUrl;
        this.estado = estado;
    }

    public Long getTransacaoId() { return transacaoId; }
    public void setTransacaoId(Long transacaoId) { this.transacaoId = transacaoId; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getRedirectUrl() { return redirectUrl; }
    public void setRedirectUrl(String redirectUrl) { this.redirectUrl = redirectUrl; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
