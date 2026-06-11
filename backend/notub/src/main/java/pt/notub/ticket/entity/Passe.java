package pt.notub.ticket.entity;

import pt.notub.tariff.entity.ModalidadePasse;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Passe extends TituloTransporte {

    private LocalDateTime inicio;
    private LocalDateTime fim;

    @Enumerated(EnumType.STRING)
    private ModalidadePasse modalidade;

    public Passe() {}

    // Getters and Setters
    public LocalDateTime getInicio() { return inicio; }
    public void setInicio(LocalDateTime inicio) { this.inicio = inicio; }
    public LocalDateTime getFim() { return fim; }
    public void setFim(LocalDateTime fim) { this.fim = fim; }
    public ModalidadePasse getModalidade() { return modalidade; }
    public void setModalidade(ModalidadePasse modalidade) { this.modalidade = modalidade; }
}
