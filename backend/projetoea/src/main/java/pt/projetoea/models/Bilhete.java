package pt.projetoea.models;

import jakarta.persistence.*;

@Entity
public class Bilhete extends TituloTransporte {

    private boolean usado;

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
