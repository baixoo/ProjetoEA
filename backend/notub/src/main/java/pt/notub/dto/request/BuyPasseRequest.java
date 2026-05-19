package pt.notub.dto.request;

import pt.notub.models.ModalidadePasse;

public class BuyPasseRequest {
    private ModalidadePasse modalidade;

    public ModalidadePasse getModalidade() {
        return modalidade;
    }

    public void setModalidade(ModalidadePasse modalidade) {
        this.modalidade = modalidade;
    }
}
