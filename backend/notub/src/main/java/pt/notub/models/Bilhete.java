package pt.notub.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
public class Bilhete extends TituloTransporte {

    private boolean usado;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "utilizador_id")
    private Utilizador utilizador;

    public Bilhete() {}

    // Getters and Setters
    public boolean isUsado() { return usado; }
    public void setUsado(boolean usado) { this.usado = usado; }
    public Utilizador getUtilizador() { return utilizador; }
    public void setUtilizador(Utilizador utilizador) { this.utilizador = utilizador; }
}
