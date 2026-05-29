package pt.notub.ticket.dto;

import java.util.List;

public class BuyTicketRequest {
    private int quantidade;
    private List<Long> zonaIds;

    public BuyTicketRequest() {}

    public int getQuantidade() { return quantidade; }
    public void setQuantidade(int quantidade) { this.quantidade = quantidade; }
    public List<Long> getZonaIds() { return zonaIds; }
    public void setZonaIds(List<Long> zonaIds) { this.zonaIds = zonaIds; }
}
