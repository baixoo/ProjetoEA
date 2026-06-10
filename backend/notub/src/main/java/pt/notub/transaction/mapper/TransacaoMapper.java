package pt.notub.transaction.mapper;

import pt.notub.payment.entity.Transacao;
import pt.notub.transaction.dto.TransacaoDTO;

import java.util.List;
import java.util.stream.Collectors;

public final class TransacaoMapper {

    private TransacaoMapper() {}

    public static TransacaoDTO toDTO(Transacao t) {
        if (t == null) return null;
        TransacaoDTO dto = new TransacaoDTO();
        dto.setId(t.getId());
        dto.setDataHora(t.getDataHora());
        dto.setReferenciaExterna(t.getReferenciaExterna());
        dto.setEstadoPagamento(t.getEstadoPagamento());
        dto.setMetodoPagamento(t.getMetodoPagamento());
        dto.setTipoProduto(t.getTipoProduto());
        dto.setValor(t.getValor());
        dto.setQuantidade(t.getQuantidade());
        dto.setModalidade(t.getModalidade());
        return dto;
    }

    public static List<TransacaoDTO> toDTOList(List<Transacao> transacoes) {
        if (transacoes == null) return null;
        return transacoes.stream().map(TransacaoMapper::toDTO).collect(Collectors.toList());
    }
}
