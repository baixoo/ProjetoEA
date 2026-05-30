package pt.notub.models;

import jakarta.persistence.*;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class TituloTransporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "zona_id")
    private Zona zona;

    public TituloTransporte() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Zona getZona() { return zona; }
    public void setZona(Zona zona) { this.zona = zona; }
}
