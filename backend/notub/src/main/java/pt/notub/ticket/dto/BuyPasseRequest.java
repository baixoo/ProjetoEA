package pt.notub.ticket.dto;

import pt.notub.models.ModalidadePasse;

public class BuyPasseRequest {
    private ModalidadePasse modalidade;
    private Long zonaId;

    public BuyPasseRequest() {}

    public ModalidadePasse getModalidade() { return modalidade; }
    public void setModalidade(ModalidadePasse modalidade) { this.modalidade = modalidade; }
    public Long getZonaId() { return zonaId; }
    public void setZonaId(Long zonaId) { this.zonaId = zonaId; }
}
