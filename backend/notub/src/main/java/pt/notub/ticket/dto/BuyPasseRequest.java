package pt.notub.ticket.dto;

import pt.notub.models.ModalidadePasse;

public class BuyPasseRequest {
    private ModalidadePasse modalidade;
    private Long zonaId;
    private int mesInicio;  // 1-12
    private int anoInicio;

    public BuyPasseRequest() {}

    public ModalidadePasse getModalidade() { return modalidade; }
    public void setModalidade(ModalidadePasse modalidade) { this.modalidade = modalidade; }
    public Long getZonaId() { return zonaId; }
    public void setZonaId(Long zonaId) { this.zonaId = zonaId; }
    public int getMesInicio() { return mesInicio; }
    public void setMesInicio(int mesInicio) { this.mesInicio = mesInicio; }
    public int getAnoInicio() { return anoInicio; }
    public void setAnoInicio(int anoInicio) { this.anoInicio = anoInicio; }
}
