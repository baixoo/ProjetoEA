package pt.notub.ticket.dto;

import pt.notub.tariff.entity.ModalidadePasse;

import java.time.LocalDateTime;

public class PasseDTO extends TituloTransporteDTO {
    private LocalDateTime inicio;
    private LocalDateTime fim;
    private ModalidadePasse modalidade;

    public PasseDTO() {}

    public LocalDateTime getInicio() { return inicio; }
    public void setInicio(LocalDateTime inicio) { this.inicio = inicio; }
    public LocalDateTime getFim() { return fim; }
    public void setFim(LocalDateTime fim) { this.fim = fim; }
    public ModalidadePasse getModalidade() { return modalidade; }
    public void setModalidade(ModalidadePasse modalidade) { this.modalidade = modalidade; }
}
