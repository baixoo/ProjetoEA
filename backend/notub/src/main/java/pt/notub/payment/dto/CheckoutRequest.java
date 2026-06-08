package pt.notub.payment.dto;

public class CheckoutRequest {

    private String metodoPagamento;
    private String tipoProduto;
    private Integer quantidade;
    private String modalidade;
    private Long zonaId;
    private Double valor;
    private String telefone;
    private int mesInicio;
    private int anoInicio;

    public CheckoutRequest() {}

    public String getMetodoPagamento() { return metodoPagamento; }
    public void setMetodoPagamento(String metodoPagamento) { this.metodoPagamento = metodoPagamento; }
    public String getTipoProduto() { return tipoProduto; }
    public void setTipoProduto(String tipoProduto) { this.tipoProduto = tipoProduto; }
    public Integer getQuantidade() { return quantidade; }
    public void setQuantidade(Integer quantidade) { this.quantidade = quantidade; }
    public String getModalidade() { return modalidade; }
    public void setModalidade(String modalidade) { this.modalidade = modalidade; }
    public Long getZonaId() { return zonaId; }
    public void setZonaId(Long zonaId) { this.zonaId = zonaId; }
    public Double getValor() { return valor; }
    public void setValor(Double valor) { this.valor = valor; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public int getMesInicio() { return mesInicio; }
    public void setMesInicio(int mesInicio) { this.mesInicio = mesInicio; }
    public int getAnoInicio() { return anoInicio; }
    public void setAnoInicio(int anoInicio) { this.anoInicio = anoInicio; }
}
