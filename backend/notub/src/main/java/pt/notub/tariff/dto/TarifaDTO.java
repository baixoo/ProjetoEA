package pt.notub.tariff.dto;

import pt.notub.models.ModalidadePasse;
import pt.notub.models.TipoUtilizador;

public class TarifaDTO {

    private Long id;
    private float valor;
    private TipoUtilizador tipoUtilizador;
    private ModalidadePasse modalidade;
    private int nrZonas;

    public TarifaDTO() {}

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
