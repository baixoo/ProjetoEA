package pt.notub.ticket.dto;

import pt.notub.zone.dto.ZonaDTO;

public class BilheteDTO extends TituloTransporteDTO {

    private boolean usado;

    public BilheteDTO() {}

    // Getters e Setters de Id e Zona estão na classe pai TituloTransporteDTO
    public boolean isUsado() { return usado; }
    public void setUsado(boolean usado) { this.usado = usado; }
}
