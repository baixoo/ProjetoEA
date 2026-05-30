package pt.notub.ticket.dto;

import pt.notub.models.ModalidadePasse;
import pt.notub.zone.dto.ZonaDTO;

import java.time.LocalDateTime;

public class PasseDTO {

    private Long id;
    private LocalDateTime inicio;
    private LocalDateTime fim;
    private ModalidadePasse modalidade;
    private ZonaDTO zona;

    public PasseDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDateTime getInicio() { return inicio; }
    public void setInicio(LocalDateTime inicio) { this.inicio = inicio; }
    public LocalDateTime getFim() { return fim; }
    public void setFim(LocalDateTime fim) { this.fim = fim; }
    public ModalidadePasse getModalidade() { return modalidade; }
    public void setModalidade(ModalidadePasse modalidade) { this.modalidade = modalidade; }
    public ZonaDTO getZona() { return zona; }
    public void setZona(ZonaDTO zona) { this.zona = zona; }
}
