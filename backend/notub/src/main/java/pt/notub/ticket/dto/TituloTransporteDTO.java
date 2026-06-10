package pt.notub.ticket.dto;

import pt.notub.zone.dto.ZonaDTO;

public abstract class TituloTransporteDTO {

    private Long id;
    private ZonaDTO zona;

    public TituloTransporteDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ZonaDTO getZona() { return zona; }
    public void setZona(ZonaDTO zona) { this.zona = zona; }
}