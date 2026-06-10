package pt.notub.transaction.dto;

import pt.notub.payment.entity.EstadoPagamento;
import pt.notub.payment.entity.MetodoPagamento;
import pt.notub.payment.entity.TipoProduto;

import java.time.LocalDateTime;

public class TransacaoDTO {

    private Long id;
    private LocalDateTime dataHora;
    private String referenciaExterna;
    private EstadoPagamento estadoPagamento;
    private MetodoPagamento metodoPagamento;
    private TipoProduto tipoProduto;
    private Double valor;
    private Integer quantidade;
    private String modalidade;

    public TransacaoDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }
    public String getReferenciaExterna() { return referenciaExterna; }
    public void setReferenciaExterna(String referenciaExterna) { this.referenciaExterna = referenciaExterna; }
    public EstadoPagamento getEstadoPagamento() { return estadoPagamento; }
    public void setEstadoPagamento(EstadoPagamento estadoPagamento) { this.estadoPagamento = estadoPagamento; }
    public MetodoPagamento getMetodoPagamento() { return metodoPagamento; }
    public void setMetodoPagamento(MetodoPagamento metodoPagamento) { this.metodoPagamento = metodoPagamento; }
    public TipoProduto getTipoProduto() { return tipoProduto; }
    public void setTipoProduto(TipoProduto tipoProduto) { this.tipoProduto = tipoProduto; }
    public Double getValor() { return valor; }
    public void setValor(Double valor) { this.valor = valor; }
    public Integer getQuantidade() { return quantidade; }
    public void setQuantidade(Integer quantidade) { this.quantidade = quantidade; }
    public String getModalidade() { return modalidade; }
    public void setModalidade(String modalidade) { this.modalidade = modalidade; }
}
