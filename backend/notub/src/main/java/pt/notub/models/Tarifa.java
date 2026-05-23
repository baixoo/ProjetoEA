package pt.notub.models;

import jakarta.persistence.*;

@Entity
public class Tarifa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private float valor;

    @Enumerated(EnumType.STRING)
    private TipoUtilizador tipoUtilizador;

    @Enumerated(EnumType.STRING)
    private ModalidadePasse modalidade;

    @Column(name = "nr_zonas")
    private int nrZonas;

    public Tarifa() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public float getValor() { return valor; }
    public void setValor(float valor) { this.valor = valor; }
    public TipoUtilizador getTipoUtilizador() { return tipoUtilizador; }
    public void setTipoUtilizador(TipoUtilizador tipoUtilizador) { this.tipoUtilizador = tipoUtilizador; }
    public ModalidadePasse getModalidade() { return modalidade; }
    public void setModalidade(ModalidadePasse modalidade) { this.modalidade = modalidade; }
    public int getNrZonas() { return nrZonas; }
    public void setNrZonas(int nrZonas) { this.nrZonas = nrZonas; }
}
