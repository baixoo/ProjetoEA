package pt.notub.ticket.dto;

import pt.notub.zone.dto.ZonaDTO;

import java.util.List;

public class BilheteDTO {

    private Long id;
    private boolean usado;
    private List<ZonaDTO> zonas;

    public BilheteDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public boolean isUsado() { return usado; }
    public void setUsado(boolean usado) { this.usado = usado; }
    public List<ZonaDTO> getZonas() { return zonas; }
    public void setZonas(List<ZonaDTO> zonas) { this.zonas = zonas; }
}
