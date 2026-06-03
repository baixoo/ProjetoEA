package pt.notub.ticket.dto;

import pt.notub.zone.dto.ZonaDTO;

public class BilheteDTO {

    private Long id;
    private boolean usado;
    private ZonaDTO zona;

    public BilheteDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public boolean isUsado() { return usado; }
    public void setUsado(boolean usado) { this.usado = usado; }
    public ZonaDTO getZona() { return zona; }
    public void setZona(ZonaDTO zona) { this.zona = zona; }
}
