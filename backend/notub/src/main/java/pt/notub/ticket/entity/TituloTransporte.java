package pt.notub.ticket.entity;

import pt.notub.zone.entity.Zona;
import pt.notub.user.entity.Utilizador;

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

    @Enumerated(EnumType.STRING)
    private TipoTituloTransporte tipo;

    @ManyToOne
    @JoinColumn(name = "utilizador_id")
    private Utilizador utilizador;

    public TituloTransporte() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Zona getZona() { return zona; }
    public void setZona(Zona zona) { this.zona = zona; }
    public TipoTituloTransporte getTipo() { return tipo; }
    public void setTipo(TipoTituloTransporte tipo) { this.tipo = tipo; }
    public Utilizador getUtilizador() { return utilizador; }
    public void setUtilizador(Utilizador utilizador) { this.utilizador = utilizador; }
}
