package pt.projetoea.models;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class TituloTransporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(mappedBy = "titulo")
    private List<ViagemUtilizador> viagens;

    @ManyToMany
    @JoinTable(
        name = "titulo_zona",
        joinColumns = @JoinColumn(name = "titulo_id"),
        inverseJoinColumns = @JoinColumn(name = "zona_id")
    )
    private List<Zona> zonas;

    public TituloTransporte() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public List<ViagemUtilizador> getViagens() { return viagens; }
    public void setViagens(List<ViagemUtilizador> viagens) { this.viagens = viagens; }
    public List<Zona> getZonas() { return zonas; }
    public void setZonas(List<Zona> zonas) { this.zonas = zonas; }
}
