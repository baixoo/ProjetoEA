package pt.notub.ticket.dto;

public class BuyTicketRequest {
    private int quantidade;
    private Long zonaId;

    public BuyTicketRequest() {}

    public int getQuantidade() { return quantidade; }
    public void setQuantidade(int quantidade) { this.quantidade = quantidade; }
    public Long getZonaId() { return zonaId; }
    public void setZonaId(Long zonaId) { this.zonaId = zonaId; }
}
