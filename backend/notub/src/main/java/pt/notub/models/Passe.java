package pt.notub.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Passe extends TituloTransporte {

    private LocalDateTime inicio;
    private LocalDateTime fim;

    @OneToOne
    @JoinColumn(name = "utilizador_id")
    private Utilizador utilizador;

    @Enumerated(EnumType.STRING)
    private ModalidadePasse modalidade;

    public Passe() {}

    // Getters and Setters
    public LocalDateTime getInicio() { return inicio; }
    public void setInicio(LocalDateTime inicio) { this.inicio = inicio; }
    public LocalDateTime getFim() { return fim; }
    public void setFim(LocalDateTime fim) { this.fim = fim; }
    public Utilizador getUtilizador() { return utilizador; }
    public void setUtilizador(Utilizador utilizador) { this.utilizador = utilizador; }
    public ModalidadePasse getModalidade() { return modalidade; }
    public void setModalidade(ModalidadePasse modalidade) { this.modalidade = modalidade; }
}
