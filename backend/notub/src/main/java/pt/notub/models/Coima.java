package pt.notub.models;

import jakarta.persistence.*;

@Entity
public class Coima {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private float valor;

    public Coima() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public float getValor() { return valor; }
    public void setValor(float valor) { this.valor = valor; }
}
