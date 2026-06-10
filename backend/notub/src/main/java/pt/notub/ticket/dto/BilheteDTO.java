package pt.notub.ticket.dto;

public class BilheteDTO extends TituloTransporteDTO {

    private boolean usado;

    public BilheteDTO() {}

    // Getters e Setters de Id e Zona estão na classe pai TituloTransporteDTO
    public boolean isUsado() { return usado; }
    public void setUsado(boolean usado) { this.usado = usado; }
}
