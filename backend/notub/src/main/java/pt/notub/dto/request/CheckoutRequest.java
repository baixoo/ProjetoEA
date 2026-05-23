package pt.notub.dto.request;

import java.util.List;

public class CheckoutRequest {
    private String metodoPagamento;
    private String tipoProduto;
    private Integer quantidade;
    private String modalidade;
    private List<Long> zonaIds;
    private Double valor;
    private String telefone;

    public CheckoutRequest() {}

    public String getMetodoPagamento() { return metodoPagamento; }
    public void setMetodoPagamento(String metodoPagamento) { this.metodoPagamento = metodoPagamento; }
    public String getTipoProduto() { return tipoProduto; }
    public void setTipoProduto(String tipoProduto) { this.tipoProduto = tipoProduto; }
    public Integer getQuantidade() { return quantidade; }
    public void setQuantidade(Integer quantidade) { this.quantidade = quantidade; }
    public String getModalidade() { return modalidade; }
    public void setModalidade(String modalidade) { this.modalidade = modalidade; }
    public List<Long> getZonaIds() { return zonaIds; }
    public void setZonaIds(List<Long> zonaIds) { this.zonaIds = zonaIds; }
    public Double getValor() { return valor; }
    public void setValor(Double valor) { this.valor = valor; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
}
