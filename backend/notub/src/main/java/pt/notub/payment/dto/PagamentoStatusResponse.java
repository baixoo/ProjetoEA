package pt.notub.payment.dto;

public class PagamentoStatusResponse {
    private Long transacaoId;
    private String estado;
    private String pagamentoExternoStatus;
    private boolean tituloCriado;

    public PagamentoStatusResponse() {}

    public Long getTransacaoId() { return transacaoId; }
    public void setTransacaoId(Long transacaoId) { this.transacaoId = transacaoId; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getPagamentoExternoStatus() { return pagamentoExternoStatus; }
    public void setPagamentoExternoStatus(String pagamentoExternoStatus) { this.pagamentoExternoStatus = pagamentoExternoStatus; }
    public boolean isTituloCriado() { return tituloCriado; }
    public void setTituloCriado(boolean tituloCriado) { this.tituloCriado = tituloCriado; }
}
