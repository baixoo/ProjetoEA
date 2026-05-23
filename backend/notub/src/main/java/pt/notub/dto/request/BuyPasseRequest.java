package pt.notub.dto.request;

import pt.notub.models.ModalidadePasse;
import java.util.List;

public class BuyPasseRequest {
    private ModalidadePasse modalidade;
    private List<Long> zonaIds;

    public BuyPasseRequest() {}

    public ModalidadePasse getModalidade() { return modalidade; }
    public void setModalidade(ModalidadePasse modalidade) { this.modalidade = modalidade; }
    public List<Long> getZonaIds() { return zonaIds; }
    public void setZonaIds(List<Long> zonaIds) { this.zonaIds = zonaIds; }
}
