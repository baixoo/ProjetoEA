package pt.notub.network.entity;

import pt.notub.zone.entity.Zona;

import pt.notub.vehicle.entity.Point;

import jakarta.persistence.*;

@Entity
public class Paragem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;

    @Embedded
    private Point localizacao;

    @ManyToOne
    @JoinColumn(name = "zona_id")
    private Zona zona;

    public Paragem() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public Point getLocalizacao() { return localizacao; }
    public void setLocalizacao(Point localizacao) { this.localizacao = localizacao; }
    public Zona getZona() { return zona; }
    public void setZona(Zona zona) { this.zona = zona; }
}
