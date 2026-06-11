package pt.notub.ticket.entity;

import jakarta.persistence.*;

@Entity
public class Bilhete extends TituloTransporte {

    private boolean usado;

    public Bilhete() {}

    // Getters and Setters
    public boolean isUsado() { return usado; }
    public void setUsado(boolean usado) { this.usado = usado; }
}
